package com.youversion.platform.core.highlights.domain

import androidx.annotation.VisibleForTesting
import co.touchlab.kermit.Logger
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
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
    val accountId: String? = null,
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
 * Coordinates Bible highlights between the local [BibleHighlightCache] and the YouVersion highlights API.
 *
 * Reads are served from the observable cache and refreshed per-chapter from the server (throttled). Writes update
 * the cache immediately (optimistically) and are queued for the server with retry/backoff so they survive transient
 * failures within the session. The cache and the queue are held in memory only: they do not survive process death, so
 * a write that has not yet reached the server is lost if the process is killed before it syncs.
 *
 * Each queued write is bound to the account that was signed in when it was made. If the signed-in account changes
 * before the write syncs, the write is dropped rather than sent, so one user's highlights can never land on another
 * user's account.
 *
 * The cache is cleared automatically whenever the signed-in account changes: [accountIdChanges] is observed and each
 * change triggers [reset], so one user's cached highlights cannot be read after a sign-out, sign-in, or account switch.
 */
class BibleHighlightsRepository internal constructor(
    private val api: HighlightsApi = HighlightsEndpoints,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val cache: BibleHighlightCache = BibleHighlightCache.shared,
    private val currentAccountId: () -> String? = { YouVersionApi.users.currentUserId },
    accountIdChanges: Flow<String?> =
        YouVersionPlatformConfiguration.configState.map { currentAccountId() },
) {
    /**
     * Test-only constructor exposing just the [api] dependency so consumer-module tests can supply a mock while the
     * cache, scope, and account wiring keep their production defaults. The primary constructor stays internal because
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
    // previous account's writes stop retrying after a sign-out rather than looping forever. Mirrors the Swift SDK's
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
        scope.launch {
            accountIdChanges
                .distinctUntilChanged()
                .drop(1)
                .collect { reset() }
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
            ),
        )
    }

    /**
     * Removes the highlights on each of [references], updating the cache immediately and syncing to the server.
     */
    fun removeHighlights(references: List<BibleReference>) {
        val normalizedReferences = references.map { it.verseLevelReference() }
        cache.removeHighlights(normalizedReferences)
        queueOperation(
            PendingHighlightOperation(
                references = normalizedReferences,
                change = HighlightChange.Remove,
            ),
        )
    }

    /**
     * Removes the highlights on [references] that currently carry [matchingColor], after loading each reference's
     * chapter so the match reflects the server's highlights rather than a cold cache. Runs on the repository's own load
     * scope so the removal still completes if the caller (for example a ViewModel) is torn down while the load is in
     * flight; it is cancelled with other loads when [reset] runs on an account change, which is intended since the
     * removal should not outlive the account that requested it.
     */
    fun removeHighlights(
        references: List<BibleReference>,
        matchingColor: String,
    ) {
        if (references.isEmpty()) return
        loadScope.launch {
            references.forEach { awaitHighlightsForChapterLoaded(it) }
            val matchingReferences =
                references
                    .flatMap { selection ->
                        highlights(overlapping = selection)
                            .filter { isSameHexColor(it.hexColor, matchingColor) }
                            .map { it.bibleReference }
                            .filter { selection.contains(it) }
                    }.distinct()
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
        val normalizedReferences = references.map { it.verseLevelReference() }
        cache.updateHighlightColors(normalizedReferences, newColor)
        queueOperation(
            PendingHighlightOperation(
                references = normalizedReferences,
                change = HighlightChange.SetColor(color = newColor),
            ),
        )
    }

    /**
     * Clears all cached highlights and per-chapter load state, and cancels any in-flight chapter loads so a load that
     * was already running cannot repopulate the cache after it is cleared. This runs automatically whenever the
     * signed-in account changes; it is also exposed for callers that need to clear the cache explicitly.
     *
     * The queued sync operations are dropped and the operation generation is bumped so that a batch a processor is
     * already mid-flight on drops its failed operations instead of re-queuing them: a previous account's writes stop
     * retrying rather than looping forever after a sign-out. Any reference already sent before the account changed is
     * additionally guarded at send time — each queued write is bound to the account that made it and is not dispatched
     * once the signed-in account differs — so one user's highlights can never land on another user's account. To flush
     * queued writes before signing out, call [flushPendingWrites] while the current account is still authenticated.
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
     * Seeds the cache with [highlights] directly, bypassing the server sync queue so they surface in [highlights] as if
     * already loaded. Test-only: it lets a consumer-module test place highlights in the read path without exercising the
     * network.
     */
    @VisibleForTesting
    fun seedCachedHighlights(highlights: List<BibleHighlight>) {
        cache.addHighlights(highlights)
    }

    /**
     * Clears the cache and its in-flight load bookkeeping without touching the sync queue. Test-only: it resets the
     * shared cache between tests.
     */
    @VisibleForTesting
    fun clearCachedHighlights() {
        cache.clear()
    }

    /**
     * Sends every queued highlight change to the server and suspends until the queue has drained. Call this before
     * signing out or switching accounts so queued writes are flushed while the current account is still authenticated.
     * Because the queue is held in memory only, this is also how a caller avoids losing queued writes when the process
     * is about to be torn down.
     * Failed writes retry indefinitely, so a permanently failing write keeps this suspended; wrap the call in
     * [kotlinx.coroutines.withTimeout] to bound how long it may block. Note that it joins the in-progress processor
     * rather than interrupting an active retry backoff.
     *
     * Returns without draining if [scope] is no longer active: a cancelled scope can never run the processor, so
     * waiting on it would spin forever. Any writes still queued at that point are lost with the process.
     *
     * Draining is judged complete only when the queue is empty *and* no processor is mid-batch, both read under
     * [queueLock]. Checking the queue alone would let this return in the window where a batch has been taken from the
     * queue but its writes are still in flight.
     */
    suspend fun flushPendingWrites() {
        while (scope.isActive) {
            val job =
                queueLock.withLock {
                    if (queuedOperations.value.isEmpty() && !isProcessingQueue) {
                        return
                    }
                    processingJobLocked()
                }
            job.start()
            job.join()
        }
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

    private fun queueOperation(operation: PendingHighlightOperation) {
        val stamped = operation.copy(accountId = currentAccountId())
        val job =
            queueLock.withLock {
                queuedOperations.value = queuedOperations.value + stamped
                processingJobLocked()
            }
        job.start()
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
                for (operation in batch) {
                    var thrownError: Throwable? = null
                    val failedReferences =
                        try {
                            processOperation(operation)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Logger.e(e) { "Highlight operation ${operation.id} threw" }
                            thrownError = e
                            operation.references
                        }
                    if (failedReferences.isEmpty()) {
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
                                references = failedReferences,
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

                if (failed.isEmpty()) {
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
     * The signed-in account is re-checked before each send. If it has changed since [operation] was queued, the send
     * loop stops so no write can reach a different account; references already sent under the original account are still
     * promoted, and the rest are dropped without retrying.
     */
    private suspend fun processOperation(operation: PendingHighlightOperation): List<BibleReference> {
        val change = operation.change
        val syncedReferences = mutableListOf<BibleReference>()
        val failedReferences = mutableListOf<BibleReference>()
        for (reference in operation.references) {
            if (change is HighlightChange.SetColor) {
                cache.awaitChapterLoaded(reference)
            }
            if (operation.accountId != currentAccountId()) {
                Logger.w { "Dropping highlight operation ${operation.id} queued under a different account" }
                break
            }
            val passageId = reference.asUSFM
            val succeeded =
                when (change) {
                    is HighlightChange.SetColor -> syncHighlight(reference, passageId, change.color)
                    HighlightChange.Remove ->
                        api.deleteHighlight(reference.versionId, passageId)
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
        return failedReferences
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

    private suspend fun loadChapterFromServer(
        chapter: BibleReference,
        load: BibleHighlightCache.ChapterLoad,
    ) {
        try {
            val passageId = chapter.chapterUSFM
            val serverHighlights =
                api
                    .highlights(versionId = chapter.versionId, passageId = passageId)
                    .mapNotNull { it.bibleHighlight() }
            currentCoroutineContext().ensureActive()
            cache.applyServerHighlights(chapter = chapter, highlights = serverHighlights, load = load)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e) { "Failed to load highlights for chapter $chapter" }
        } finally {
            cache.unmarkChapterAsLoading(chapter, load)
        }
    }

    private fun Highlight.bibleHighlight(): BibleHighlight? {
        val reference = BibleReference.unvalidatedReference(usfm = passageId, versionId = bibleId)
        if (reference == null) {
            Logger.w { "Ignoring highlight with unparseable passage id: $passageId" }
            return null
        }
        return BibleHighlight(bibleReference = reference, hexColor = hexWithHash(color))
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
