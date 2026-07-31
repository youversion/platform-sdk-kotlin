package com.youversion.platform.core.highlights.domain

import androidx.annotation.VisibleForTesting
import co.touchlab.kermit.Logger
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.api.notPermitted
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.api.HighlightsApi
import com.youversion.platform.core.highlights.api.HighlightsEndpoints
import com.youversion.platform.core.highlights.models.BibleHighlight
import com.youversion.platform.core.highlights.models.Highlight
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.pow

/**
 * The change a [PendingHighlightOperation] applies to its references.
 *
 * Modeled as a sealed type so that setting a highlight's color always carries a color while removing one never does: a
 * colorless [SetColor] cannot be constructed, so no queued write can ever send a blank color to the server.
 */
sealed interface HighlightChange {
    /**
     * Sets highlights to [color]. Adding and recoloring both mean "ensure a highlight of this color exists", so they
     * share this type and sync identically.
     */
    data class SetColor(
        val color: String,
    ) : HighlightChange

    /** Removes existing highlights. */
    data object Remove : HighlightChange
}

/**
 * A highlight change that has been applied to the local cache and is queued to be synced to the server.
 */
data class PendingHighlightOperation(
    val references: List<BibleReference>,
    val change: HighlightChange,
    val id: UUID = UUID.randomUUID(),
    val timestamp: Date = Date(),
    val retryCount: Int = 0,
    val sessionId: String? = null,
)

/**
 * The outcome of the most recent attempt to sync a [PendingHighlightOperation] to the server.
 *
 * Mirrors the Swift SDK's `OperationResult`: it is keyed by [operationId] and refreshed each time the operation is
 * processed, so callers can inspect whether a specific write succeeded, why it failed, and how many times it has retried.
 */
data class OperationResult(
    val operationId: UUID,
    val isSuccess: Boolean,
    val error: Throwable? = null,
    val retryCount: Int = 0,
)

/**
 * How each reference in an operation fared, splitting failures the server may yet accept from ones it never will.
 *
 * References that reached the server are promoted in the cache as they are sent, so they are not reported here.
 * [failedReferences] are retryable — a transient error, so they are requeued with backoff. [rejectedReferences] were
 * refused because the user is not permitted to write highlights, which no amount of retrying changes, so they are
 * dropped from the queue and reconciled from the server.
 */
private data class OperationOutcome(
    val failedReferences: List<BibleReference> = emptyList(),
    val rejectedReferences: List<BibleReference> = emptyList(),
)

/**
 * Coordinates Bible highlights between the local [BibleHighlightCache] and the YouVersion highlights API.
 *
 * Reads are served from the observable cache and refreshed per-chapter from the server (throttled). Writes update
 * the cache immediately (optimistically) and are queued for the server with retry/backoff so they survive transient
 * failures within the session. The cache and the queue are held in memory only: they do not survive process death, so
 * a write that has not yet reached the server is lost if the process is killed before it syncs.
 *
 * A write the server refuses for lack of permission is not retried, since no retry would change the answer. The
 * refusal applies to the whole account, so every other queued write is dropped along with it, and the references they
 * touched are reconciled from the server: their chapters are reloaded and the server's rows replace the optimistic
 * ones. Nothing is discarded until that reload resolves. A reload that merely fails leaves the optimistic highlight on
 * screen rather than erasing it, for a later load to reconcile; a reload the server refuses in turn clears every
 * cached highlight, since that refusal means the user has not granted this app access to their highlights at all.
 *
 * Each queued write is bound to the session that made it via [YouVersionApi.currentSessionId], read before the write
 * touches the cache, and is dropped rather than sent if the session changes before it syncs. The auth token is read
 * from the global configuration at request-build time, so an unbound write would reach whichever account is signed in
 * when it finally sends.
 *
 * The cache is cleared on every session change: [sessionIdChanges] is observed and each change triggers [reset], so
 * one user's cached highlights cannot be read after a sign-out, sign-in, or account switch. Emissions are compared
 * against the session read at construction rather than the first emission, which can already be the new session.
 */
class BibleHighlightsRepository internal constructor(
    private val api: HighlightsApi = HighlightsEndpoints,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val cache: BibleHighlightCache = BibleHighlightCache.shared,
    private val currentSessionId: () -> String? = { YouVersionApi.currentSessionId },
    sessionIdChanges: Flow<String?> =
        YouVersionPlatformConfiguration.configState.map { currentSessionId() },
) {
    /**
     * Test-only constructor exposing just the [api] dependency so consumer-module tests can supply a mock while the
     * cache, scope, and session wiring keep their production defaults. The primary constructor stays internal because
     * it exposes the internal [BibleHighlightCache] type.
     */
    @VisibleForTesting
    constructor(api: HighlightsApi) : this(api = api, cache = BibleHighlightCache.shared)

    private val loadScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))

    private val queueLock = ReentrantLock()
    private var isProcessingQueue = false
    private var processingJob: Job? = null

    // Bumped by [reset] to invalidate the batch a processor is mid-flight on: a batch captures this at its start and,
    // if it no longer matches when the batch finishes, drops its failed operations instead of re-queuing them so a
    // previous session's writes stop retrying after a sign-out rather than looping forever. Mirrors the Swift SDK's
    // operationGeneration.
    private var operationGeneration = 0
    private val queuedOperations = MutableStateFlow<List<PendingHighlightOperation>>(emptyList())
    private val operationResultsState = MutableStateFlow<Map<UUID, OperationResult>>(emptyMap())

    /**
     * The observable list of highlights. UI layers should collect this and filter to the references they render. The
     * cache's internal sync bookkeeping (pending vs. synced state) is intentionally not exposed here; consumers see only
     * the highlights themselves.
     */
    val highlights: StateFlow<List<BibleHighlight>> =
        MappedStateFlow(cache.highlights) { cached -> cached.map { it.highlight } }

    /**
     * The number of highlight changes queued for the server, including any awaiting a retry. Collect this to drive a
     * sync indicator. The count drops to zero while a batch is in flight and rises again only if operations fail.
     */
    val pendingOperationCount: StateFlow<Int> =
        queuedOperations
            .map { operations -> operations.size }
            .stateIn(scope, SharingStarted.Eagerly, 0)

    /**
     * The number of queued highlight changes that have failed at least once and are awaiting a retry.
     */
    val failedOperationCount: StateFlow<Int> =
        combine(queuedOperations, operationResultsState) { operations, results ->
            operations.count { results[it.id]?.isSuccess == false }
        }.stateIn(scope, SharingStarted.Eagerly, 0)

    init {
        var previousSessionId = currentSessionId()
        scope.launch {
            sessionIdChanges.collect { sessionId ->
                if (previousSessionId != sessionId) {
                    previousSessionId = sessionId
                    reset()
                }
            }
        }
    }

    /**
     * Returns the cached highlights overlapping [overlapping] synchronously.
     */
    fun highlights(overlapping: BibleReference): List<BibleHighlight> = cache.highlights(overlapping = overlapping)

    /**
     * Loads the chapter containing [reference] from the server if it has not been loaded recently.
     *
     * Loading is throttled per chapter and de-duplicated while a load is in flight. Pass [forceReload] to bypass the
     * time-based throttle. It does not interrupt a load already in progress; instead, when one is in flight it waits for
     * that load to finish and then starts a single fresh load, so a forced reload always reflects server state fetched
     * after this call rather than being silently dropped. It schedules at most one follow-up load: if another load has
     * already registered by the time the in-flight one finishes, that newer load began after this call and so already
     * reflects post-call state, and this defers to it instead of chaining more reloads.
     */
    fun ensureHighlightsForChapterLoaded(
        reference: BibleReference,
        forceReload: Boolean = false,
    ) {
        val chapter =
            BibleReference(
                versionId = reference.versionId,
                bookUSFM = reference.bookUSFM,
                chapter = reference.chapter,
            )

        if (!forceReload && cache.hasRecentlyLoadedChapter(chapter)) {
            return
        }
        val load = cache.markChapterAsLoading(chapter)
        if (load == null) {
            if (forceReload) {
                loadScope.launch {
                    cache.awaitChapterLoaded(chapter)
                    cache.markChapterAsLoading(chapter)?.let { freshLoad ->
                        loadChapterFromServer(chapter, freshLoad)
                    }
                }
            }
            return
        }

        loadScope.launch { loadChapterFromServer(chapter, load) }
    }

    /**
     * Loads the chapter containing [reference] and suspends until that load finishes, so code that then reads a cached
     * query such as [highlights] sees the chapter's server highlights instead of a cold cache. Triggers the same
     * throttled, de-duplicated load as [ensureHighlightsForChapterLoaded] on the repository's own load scope — so
     * [reset] still cancels it — then awaits it. It returns without waiting when the chapter was loaded recently or no
     * load is in flight.
     */
    private suspend fun awaitHighlightsForChapterLoaded(reference: BibleReference) {
        ensureHighlightsForChapterLoaded(reference)
        cache.awaitChapterLoaded(reference)
    }

    /**
     * Adds highlights of [color] to each of [references], updating the cache immediately and syncing to the server.
     */
    fun addHighlights(
        references: List<BibleReference>,
        color: String,
    ) {
        val sessionId = currentSessionId()
        val normalizedReferences = references.map { it.verseLevelReference() }
        val highlights =
            normalizedReferences.map { reference ->
                BibleHighlight(bibleReference = reference, hexColor = color)
            }
        cache.addHighlights(highlights)
        queueOperation(
            PendingHighlightOperation(
                references = normalizedReferences,
                change = HighlightChange.SetColor(color = color),
                sessionId = sessionId,
            ),
        )
    }

    /**
     * Removes the highlights on each of [references], updating the cache immediately and syncing to the server.
     */
    fun removeHighlights(references: List<BibleReference>) {
        val sessionId = currentSessionId()
        val normalizedReferences = references.map { it.verseLevelReference() }
        cache.removeHighlights(normalizedReferences)
        queueOperation(
            PendingHighlightOperation(
                references = normalizedReferences,
                change = HighlightChange.Remove,
                sessionId = sessionId,
            ),
        )
    }

    /**
     * Removes the highlights on [references] that currently carry [matchingColor], after loading each reference's
     * chapter so the match reflects the server's highlights rather than a cold cache. Runs on the repository's own load
     * scope so the removal still completes if the caller (for example a ViewModel) is torn down while the load is in
     * flight; it is cancelled with other loads when [reset] runs on a session change, which is intended since the
     * removal should not outlive the session that requested it.
     */
    fun removeHighlights(
        references: List<BibleReference>,
        matchingColor: String,
    ) {
        if (references.isEmpty()) return
        loadScope.launch {
            references.forEach { awaitHighlightsForChapterLoaded(it) }
            val matchingReferences =
                references.filter { selection ->
                    highlights(overlapping = selection).any { isSameHexColor(it.hexColor, matchingColor) }
                }
            if (matchingReferences.isNotEmpty()) removeHighlights(matchingReferences)
        }
    }

    /**
     * Recolors the highlights on each of [references] to [newColor], updating the cache immediately and syncing to the
     * server.
     *
     * Whether each reference syncs as an update (PUT) or a create (POST) is decided when the change reaches the server,
     * from the highlight's cache state at that point, so a reference whose chapter is still loading is not classified
     * from a stale snapshot.
     */
    fun updateHighlightColors(
        references: List<BibleReference>,
        newColor: String,
    ) {
        val sessionId = currentSessionId()
        val normalizedReferences = references.map { it.verseLevelReference() }
        cache.updateHighlightColors(normalizedReferences, newColor)
        queueOperation(
            PendingHighlightOperation(
                references = normalizedReferences,
                change = HighlightChange.SetColor(color = newColor),
                sessionId = sessionId,
            ),
        )
    }

    /**
     * Clears all cached highlights and per-chapter load state, and cancels any in-flight chapter loads so a load that
     * was already running cannot repopulate the cache after it is cleared. This runs automatically whenever the
     * session changes; it is also exposed for callers that need to clear the cache explicitly.
     *
     * The queued sync operations are dropped and the operation generation is bumped so that a batch a processor is
     * already mid-flight on drops its failed operations instead of re-queuing them: a previous session's writes stop
     * retrying rather than looping forever after a sign-out. A write already in flight is additionally guarded at send
     * time, so one user's highlights can never land on another user's account.
     */
    fun reset() {
        queueLock.withLock {
            queuedOperations.value = emptyList()
            operationGeneration++
        }
        loadScope.coroutineContext.cancelChildren()
        cache.clear()
    }

    /**
     * Whether any highlight change has yet to reach the server, whether it is still queued or already in flight. Read
     * this before signing out or switching accounts to warn that the changes would be lost, since the queue is held in
     * memory only.
     *
     * A processor takes the whole queue when it claims a batch, so [pendingOperationCount] reads zero for as long as
     * that batch is in flight. This reports the in-progress processor too, and so stays true across that window.
     */
    fun hasPendingOperations(): Boolean {
        queueLock.withLock { return isProcessingQueue || queuedOperations.value.isNotEmpty() }
    }

    /**
     * Seeds the cache with [highlights] directly, bypassing the server sync queue so they surface in [highlights] as if
     * already loaded. Test-only: it lets a consumer-module test place highlights in the read path without exercising the
     * network. Public because [BibleHighlightCache] is internal to this module and so out of reach of those tests.
     */
    @VisibleForTesting
    fun seedCachedHighlights(highlights: List<BibleHighlight>) {
        cache.addHighlights(highlights)
    }

    /**
     * Clears the cache and its in-flight load bookkeeping without touching the sync queue. Test-only: the cache is a
     * process-wide singleton, so a consumer-module test resets it here between cases.
     */
    @VisibleForTesting
    fun clearCachedHighlights() {
        cache.clear()
    }

    /**
     * The result of the most recent attempt to sync the operation with [operationId], or null if it has not been
     * processed yet or its results have been cleared via [clearOperationResults].
     */
    fun operationResult(operationId: UUID): OperationResult? = operationResultsState.value[operationId]

    /**
     * Forgets every recorded [OperationResult]. This does not affect operations still queued for the server; it only
     * clears the history exposed by [operationResult].
     */
    fun clearOperationResults() {
        queueLock.withLock {
            operationResultsState.value = emptyMap()
        }
    }

    /**
     * Ensures the sync processor is running if any queued operation has previously failed. Operations already retry
     * automatically with backoff, so this only has an effect when the processor has fully drained and stopped; it does
     * not interrupt an in-progress retry backoff. Provided to mirror the Swift SDK's `retryFailedOperations`.
     */
    suspend fun retryFailedOperations() {
        val hasFailedOperations =
            queueLock.withLock {
                val results = operationResultsState.value
                queuedOperations.value.any { results[it.id]?.isSuccess == false }
            }
        if (hasFailedOperations) {
            ensureProcessing()
        }
    }

    /**
     * Appends [operation] to the queue and makes sure a processor is running.
     *
     * [operation] must already carry the session read *before* its cache mutation was applied, not one read here: a
     * session change landing between the two runs [reset], and stamping afterwards would label the write with the
     * replacement session, so the send-time guard would compare that session against itself and let one user's
     * highlight reach another's account. Reading it first means any such interleaving leaves a stale stamp, which
     * that guard drops.
     */
    private fun queueOperation(operation: PendingHighlightOperation) {
        val job =
            queueLock.withLock {
                queuedOperations.value = queuedOperations.value + operation
                processingJobLocked()
            }
        job.start()
    }

    /**
     * Abandons every write left doomed by a server refusal and reconciles the references they touched.
     *
     * A refusal is account-wide and permanent, so nothing still queued can succeed: [alreadyAbandonedOperations] and
     * everything still queued are dropped rather than sent, each recorded as refused, and every reference involved is
     * marked for reconciliation before its chapter is reloaded.
     *
     * The refused writes are undone by taking the server's state rather than by restoring the rows the cache held
     * before each change. A snapshot is only correct while nothing has touched the reference since it was captured, so
     * restoring one could erase a later change or resurrect a row the server never accepted — and because the cache
     * only ever drops remote-synced rows on a merge, such a row would survive every reload. The server never applied a
     * refused write, so its own state is what the cache should hold, whatever order the writes were made in.
     */
    private fun abandonRefusedWrites(
        references: List<BibleReference>,
        alreadyAbandonedOperations: List<PendingHighlightOperation>,
    ) {
        val queued =
            queueLock.withLock {
                queuedOperations.value.also { queuedOperations.value = emptyList() }
            }
        val abandoned = alreadyAbandonedOperations + queued
        abandoned.forEach { operation ->
            recordResult(
                OperationResult(
                    operationId = operation.id,
                    isSuccess = false,
                    error = notPermitted(),
                    retryCount = operation.retryCount,
                ),
            )
        }

        val affectedReferences = (references + abandoned.flatMap { it.references }).distinct()
        cache.markAwaitingReconcile(affectedReferences)
        affectedReferences
            .distinctBy { Triple(it.versionId, it.bookUSFM, it.chapter) }
            .forEach { ensureHighlightsForChapterLoaded(it, forceReload = true) }
    }

    private fun ensureProcessing(): Job {
        val job = queueLock.withLock { processingJobLocked() }
        job.start()
        return job
    }

    /**
     * Returns the queue-processing job, launching a fresh lazy one if none is active. Must be called while [queueLock]
     * is held so a caller can append to [queuedOperations] and claim the processor atomically; the returned job is
     * created lazily and must be started by the caller outside the lock.
     */
    private fun processingJobLocked(): Job {
        val existing = processingJob
        if (isProcessingQueue && existing != null) {
            return existing
        }
        isProcessingQueue = true
        return scope.launch(start = CoroutineStart.LAZY) { processQueue() }.also { processingJob = it }
    }

    private suspend fun processQueue() {
        val thisJob = currentCoroutineContext()[Job]
        try {
            while (true) {
                val batchState =
                    queueLock.withLock {
                        val current = queuedOperations.value
                        if (current.isEmpty()) {
                            // Observe "empty" and stop processing atomically: if a concurrent queueOperation adds an
                            // item after this, it acquires the mutex next, sees isProcessingQueue == false, and starts
                            // a fresh processor rather than deferring to this dying one.
                            isProcessingQueue = false
                            return@withLock null
                        }
                        queuedOperations.value = emptyList()
                        current to operationGeneration
                    } ?: break
                val (batch, generationAtStart) = batchState

                val failed = mutableListOf<PendingHighlightOperation>()
                var wasRefused = false
                for ((index, operation) in batch.withIndex()) {
                    var thrownError: Throwable? = null
                    val outcome =
                        try {
                            processOperation(operation)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Logger.e(e) { "Highlight operation ${operation.id} threw" }
                            thrownError = e
                            OperationOutcome(failedReferences = operation.references)
                        }

                    if (outcome.rejectedReferences.isNotEmpty()) {
                        recordResult(
                            OperationResult(
                                operationId = operation.id,
                                isSuccess = false,
                                error = notPermitted(),
                                retryCount = operation.retryCount,
                            ),
                        )
                        // The refusal is account-wide, so every write still waiting is doomed too — including ones that
                        // merely failed transiently earlier in this batch, which would now be refused rather than retried.
                        abandonRefusedWrites(
                            references = outcome.rejectedReferences + outcome.failedReferences,
                            alreadyAbandonedOperations = failed + batch.drop(index + 1),
                        )
                        wasRefused = true
                        break
                    }

                    if (outcome.failedReferences.isEmpty()) {
                        recordResult(
                            OperationResult(
                                operationId = operation.id,
                                isSuccess = true,
                                retryCount = operation.retryCount,
                            ),
                        )
                    } else {
                        val retried =
                            operation.copy(
                                references = outcome.failedReferences,
                                retryCount = operation.retryCount + 1,
                            )
                        recordResult(
                            OperationResult(
                                operationId = operation.id,
                                isSuccess = false,
                                error = thrownError ?: IllegalStateException(SERVER_OPERATION_FAILED_MESSAGE),
                                retryCount = retried.retryCount,
                            ),
                        )
                        failed.add(retried)
                    }
                }

                if (wasRefused || failed.isEmpty()) {
                    continue
                }

                val didRequeue =
                    queueLock.withLock {
                        if (operationGeneration != generationAtStart) {
                            false
                        } else {
                            queuedOperations.value = failed + queuedOperations.value
                            true
                        }
                    }
                if (didRequeue) {
                    delay(backoffMillis(failed.maxOf { it.retryCount }))
                }
            }
        } finally {
            queueLock.withLock {
                if (processingJob === thisJob) {
                    isProcessingQueue = false
                }
            }
        }
    }

    private fun recordResult(result: OperationResult) {
        queueLock.withLock {
            operationResultsState.value = operationResultsState.value + (result.operationId to result)
        }
    }

    /**
     * Sends each reference in [operation] to the server and returns the references that failed, so retries only touch
     * the references that did not succeed. A create or recolor first waits for its chapter's in-flight load to finish so
     * it is classified from server state rather than a stale snapshot; a delete targets the server by passage id and so
     * does not wait. A color change that syncs is promoted to remote-synced in the cache so a later server merge does not
     * leave a stale pending row beside the server copy; a delete that syncs stamps its tombstone so a later load can
     * clear it once the server confirms the removal.
     *
     * The session is re-checked before each send. If it has changed since [operation] was queued, the send loop stops
     * so no write can reach a different account; references already sent are still promoted, and the rest are dropped
     * without retrying.
     *
     * A reference the server refuses for lack of permission is reported as rejected rather than failed, and the send
     * loop stops: the remaining references would be refused for the same reason, so they are reported as rejected too
     * rather than each firing its own doomed request.
     */
    private suspend fun processOperation(operation: PendingHighlightOperation): OperationOutcome {
        val change = operation.change
        val syncedReferences = mutableListOf<BibleReference>()
        val failedReferences = mutableListOf<BibleReference>()
        val rejectedReferences = mutableListOf<BibleReference>()
        for ((index, reference) in operation.references.withIndex()) {
            if (change is HighlightChange.SetColor) {
                cache.awaitChapterLoaded(reference)
            }
            if (operation.sessionId != currentSessionId()) {
                Logger.w { "Dropping highlight operation ${operation.id} queued under a different session" }
                break
            }
            val passageId = reference.asUSFM
            val succeeded =
                try {
                    when (change) {
                        is HighlightChange.SetColor -> syncHighlight(reference, passageId, change.color)
                        HighlightChange.Remove ->
                            api.deleteHighlight(reference.versionId, passageId)
                    }
                } catch (e: YouVersionNetworkException) {
                    if (e.reason != YouVersionNetworkException.Reason.NOT_PERMITTED) throw e
                    Logger.w { "Dropping highlight operation ${operation.id}: not permitted to change highlights" }
                    rejectedReferences.addAll(operation.references.drop(index))
                    break
                }
            if (succeeded) {
                syncedReferences.add(reference)
            } else {
                failedReferences.add(reference)
            }
        }

        if (syncedReferences.isNotEmpty()) {
            when (change) {
                is HighlightChange.SetColor ->
                    cache.markHighlightsAsSynced(syncedReferences, notModifiedAfter = operation.timestamp)
                HighlightChange.Remove ->
                    cache.markDeletesSynced(syncedReferences)
            }
        }
        return OperationOutcome(failedReferences = failedReferences, rejectedReferences = rejectedReferences)
    }

    /**
     * Syncs a highlight of [color] for [reference] to the server, choosing create versus update from the highlight's
     * cache state at send time rather than from the change type. Both an add and a recolor mean "ensure a highlight of
     * this color exists for this reference", so they resolve identically: if the reference is already server-backed the
     * change is a PUT, otherwise a POST. This keeps a retried add — or an add superseded by a recolor that reached the
     * server first — from firing a second create for a reference the server already holds.
     *
     * The caller must await the reference's chapter load (via [BibleHighlightCache.awaitChapterLoaded]) before calling
     * this, so create-versus-update is decided from server state the load has already merged rather than from a snapshot
     * that does not yet reflect what the server holds.
     */
    private suspend fun syncHighlight(
        reference: BibleReference,
        passageId: String,
        color: String,
    ): Boolean =
        if (cache.isHighlightServerBacked(reference)) {
            api.updateHighlight(reference.versionId, passageId, hexWithoutHash(color))
        } else {
            api.createHighlight(reference.versionId, passageId, hexWithoutHash(color))
        }

    /**
     * Loads [chapter]'s highlights from the server and merges them into the cache.
     *
     * A read the server refuses for lack of permission clears every cached highlight, not just this chapter's: the
     * refusal says the user has not granted this app access to their highlights at all, so no chapter's cached copy is
     * still theirs to show. Nothing else clears on that — a session change triggers [reset], but a user who stays
     * signed in and simply lacks the grant produces no session change — so this is where it is caught.
     *
     * Every other failure leaves the cache untouched: the response never arrived, so it decides nothing. That includes
     * a 401, which means the user is signed out or their token expired rather than that they revoked access. Skipping
     * the merge also leaves the reload throttle unarmed, so the next load retries immediately instead of waiting it out.
     */
    private suspend fun loadChapterFromServer(
        chapter: BibleReference,
        load: BibleHighlightCache.ChapterLoad,
    ) {
        try {
            val passageId = chapter.chapterUSFM
            val serverHighlights =
                api
                    .highlights(versionId = chapter.versionId, passageId = passageId)
                    .flatMap { it.bibleHighlights() }
            currentCoroutineContext().ensureActive()
            cache.applyServerHighlights(chapter = chapter, highlights = serverHighlights, load = load)
        } catch (e: CancellationException) {
            throw e
        } catch (e: YouVersionNetworkException) {
            if (e.reason == YouVersionNetworkException.Reason.NOT_PERMITTED) {
                Logger.w { "Not permitted to read highlights; clearing every cached highlight" }
                cache.clear()
            } else {
                Logger.e(e) { "Failed to load highlights for chapter $chapter" }
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to load highlights for chapter $chapter" }
        } finally {
            cache.unmarkChapterAsLoading(chapter, load)
        }
    }

    /**
     * Expands a server highlight into one [BibleHighlight] per verse so the cache holds only verse-level entries. A
     * multi-verse range passage is stored as its individual verses rather than a single range entry, keeping every
     * highlight independently removable by its own verse reference under the cache's exact-reference matching.
     */
    private fun Highlight.bibleHighlights(): List<BibleHighlight> {
        val reference = BibleReference.unvalidatedReference(usfm = passageId, versionId = bibleId)
        if (reference == null) {
            Logger.w { "Ignoring highlight with unparseable passage id: $passageId" }
            return emptyList()
        }
        val hexColor = hexWithHash(color)
        val verseStart = reference.verseStart
        val verseEnd = reference.verseEnd
        if (verseStart == null || verseEnd == null || verseStart == verseEnd) {
            return listOf(BibleHighlight(bibleReference = reference, hexColor = hexColor))
        }
        return (verseStart..verseEnd).map { verse ->
            BibleHighlight(
                bibleReference =
                    BibleReference(
                        versionId = reference.versionId,
                        bookUSFM = reference.bookUSFM,
                        chapter = reference.chapter,
                        verse = verse,
                    ),
                hexColor = hexColor,
            )
        }
    }

    /**
     * The verse-level identity a highlight is keyed by in the cache and when syncing: a single verse with
     * [BibleReference.verseStart] defaulted to 1. The cache matches entries by exact [BibleReference] equality, so the
     * reference stored in the cache and the one carried on the queued operation must be canonicalized the same way or
     * later lookups (server-backed checks, sync promotion, deletes) silently miss.
     */
    private fun BibleReference.verseLevelReference(): BibleReference =
        BibleReference(
            versionId = versionId,
            bookUSFM = bookUSFM,
            chapter = chapter,
            verse = verseStart ?: 1,
        )

    private fun hexWithHash(color: String): String = if (color.startsWith("#")) color else "#$color"

    private fun hexWithoutHash(color: String): String = color.removePrefix("#")

    private fun isSameHexColor(
        first: String,
        second: String,
    ): Boolean = hexWithoutHash(first).equals(hexWithoutHash(second), ignoreCase = true)

    private fun backoffMillis(retryCount: Int): Long {
        val seconds = 2.0.pow(minOf(retryCount, MAX_BACKOFF_EXPONENT)).toLong()
        return minOf(seconds * MILLIS_PER_SECOND, MAX_BACKOFF_MILLIS)
    }

    private companion object {
        const val SERVER_OPERATION_FAILED_MESSAGE = "Highlight server operation failed"
        const val MAX_BACKOFF_EXPONENT = 5
        const val MILLIS_PER_SECOND = 1_000L
        const val MAX_BACKOFF_MILLIS = 30_000L
    }
}
