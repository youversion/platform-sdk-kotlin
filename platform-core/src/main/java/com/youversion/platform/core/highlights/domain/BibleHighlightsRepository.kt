package com.youversion.platform.core.highlights.domain

import co.touchlab.kermit.Logger
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import java.util.UUID
import kotlin.math.pow

/**
 * The change a [PendingHighlightOperation] applies to its references.
 *
 * Modeled as a sealed type so that adding or recoloring a highlight always carries a color while removing one never
 * does: a colorless [Add] or [UpdateColor] cannot be constructed, so no queued write can ever send a blank color to
 * the server.
 */
sealed interface HighlightChange {
    /** Adds highlights of [color]. */
    data class Add(
        val color: String,
    ) : HighlightChange

    /** Recolors existing highlights to [color]. */
    data class UpdateColor(
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
 * failures and offline periods.
 *
 * Each queued write is bound to the account that was signed in when it was made. If the signed-in account changes
 * before the write syncs, the write is dropped rather than sent, so one user's highlights can never land on another
 * user's account.
 */
class BibleHighlightsRepository(
    private val api: HighlightsApi = HighlightsEndpoints,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val currentAccountId: () -> String? = { YouVersionApi.users.currentUserId },
) {
    private val cache = BibleHighlightCache

    private val loadScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))

    private val enqueueJob = SupervisorJob(scope.coroutineContext[Job])
    private val enqueueScope = CoroutineScope(scope.coroutineContext + enqueueJob)

    private val pendingOperations = mutableListOf<PendingHighlightOperation>()
    private val queueMutex = Mutex()
    private var isProcessingQueue = false
    private var processingJob: Job? = null
    private val queuedOperations = MutableStateFlow<List<PendingHighlightOperation>>(emptyList())
    private val operationResults = mutableMapOf<UUID, OperationResult>()
    private val operationResultsState = MutableStateFlow<Map<UUID, OperationResult>>(emptyMap())

    /**
     * The observable list of cached highlights. UI layers should collect this and filter to the references they render.
     */
    val highlights: StateFlow<List<BibleHighlightCache.CachedHighlight>>
        get() = cache.highlights

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

    /**
     * Returns the cached highlights overlapping [overlapping] synchronously.
     */
    fun highlights(overlapping: BibleReference): List<BibleHighlight> = cache.highlights(overlapping = overlapping)

    /**
     * Loads the chapter containing [reference] from the server if it has not been loaded recently.
     *
     * Loading is throttled per chapter and de-duplicated while a load is in flight. Pass [forceReload] to bypass the
     * time-based throttle; it does not cancel or re-trigger a load that is already in progress, so if one is in flight
     * this call returns without starting another.
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
        if (!cache.markChapterAsLoading(chapter)) {
            return
        }

        loadScope.launch { loadChapterFromServer(chapter) }
    }

    /**
     * Adds highlights of [color] to each of [references], updating the cache immediately and syncing to the server.
     */
    fun addHighlights(
        references: List<BibleReference>,
        color: String,
    ) {
        val highlights =
            references.map { reference ->
                BibleHighlight(
                    bibleReference =
                        BibleReference(
                            versionId = reference.versionId,
                            bookUSFM = reference.bookUSFM,
                            chapter = reference.chapter,
                            verse = reference.verseStart ?: 1,
                        ),
                    hexColor = color,
                )
            }
        cache.addHighlights(highlights)
        queueOperation(
            PendingHighlightOperation(
                references = references,
                change = HighlightChange.Add(color = color),
            ),
        )
    }

    /**
     * Removes the highlights on each of [references], updating the cache immediately and syncing to the server.
     */
    fun removeHighlights(references: List<BibleReference>) {
        cache.removeHighlights(references)
        queueOperation(
            PendingHighlightOperation(
                references = references,
                change = HighlightChange.Remove,
            ),
        )
    }

    /**
     * Recolors the highlights on each of [references] to [newColor], updating the cache immediately and syncing to the
     * server.
     *
     * References that already have a highlight sync as an update; references that do not yet have one are created (the
     * cache treats a recolor of a missing highlight as a pending create), so they sync as a create rather than a PUT
     * against a highlight the server has never seen. The split is computed before the cache is mutated, since the
     * mutation itself adds the missing references as pending creates.
     */
    fun updateHighlightColors(
        references: List<BibleReference>,
        newColor: String,
    ) {
        val (referencesToUpdate, referencesToCreate) = references.partition { cache.containsHighlight(it) }
        cache.updateHighlightColors(references, newColor)
        if (referencesToUpdate.isNotEmpty()) {
            queueOperation(
                PendingHighlightOperation(
                    references = referencesToUpdate,
                    change = HighlightChange.UpdateColor(color = newColor),
                ),
            )
        }
        if (referencesToCreate.isNotEmpty()) {
            queueOperation(
                PendingHighlightOperation(
                    references = referencesToCreate,
                    change = HighlightChange.Add(color = newColor),
                ),
            )
        }
    }

    /**
     * Clears all cached highlights and per-chapter load state, and cancels any in-flight chapter loads so a load that
     * was already running cannot repopulate the cache after it is cleared. Call this when the user signs out.
     *
     * Pending sync operations are intentionally left in place so any writes already queued still reach the server; this
     * mirrors the Swift SDK, where reset clears only the cache.
     */
    fun reset() {
        loadScope.coroutineContext.cancelChildren()
        cache.clear()
    }

    /**
     * Sends every queued highlight change to the server and suspends until the queue has drained. Call this before
     * signing out or switching accounts so queued writes are flushed while the current account is still authenticated.
     * Failed writes retry indefinitely, so a permanently failing write keeps this suspended; wrap the call in
     * [kotlinx.coroutines.withTimeout] to bound how long it may block. Note that it joins the in-progress processor
     * rather than interrupting an active retry backoff.
     */
    suspend fun flushPendingWrites() {
        do {
            enqueueJob.children.toList().joinAll()
            ensureProcessing()?.join()
        } while (queueMutex.withLock { pendingOperations.isNotEmpty() })
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
    suspend fun clearOperationResults() {
        queueMutex.withLock {
            operationResults.clear()
            publishOperationResults()
        }
    }

    /**
     * Ensures the sync processor is running if any queued operation has previously failed. Operations already retry
     * automatically with backoff, so this only has an effect when the processor has fully drained and stopped; it does
     * not interrupt an in-progress retry backoff. Provided to mirror the Swift SDK's `retryFailedOperations`.
     */
    suspend fun retryFailedOperations() {
        val hasFailedOperations =
            queueMutex.withLock {
                pendingOperations.any { operationResults[it.id]?.isSuccess == false }
            }
        if (hasFailedOperations) {
            ensureProcessing()
        }
    }

    private fun queueOperation(operation: PendingHighlightOperation) {
        val stamped = operation.copy(accountId = currentAccountId())
        enqueueScope.launch {
            queueMutex.withLock {
                pendingOperations.add(stamped)
                pendingOperations.sortBy { it.timestamp }
                publishQueuedOperations()
            }
            ensureProcessing()
        }
    }

    private suspend fun ensureProcessing(): Job? {
        val job =
            queueMutex.withLock {
                if (isProcessingQueue) {
                    return processingJob
                }
                isProcessingQueue = true
                scope.launch(start = CoroutineStart.LAZY) { processQueue() }.also { processingJob = it }
            }
        job.start()
        return job
    }

    private suspend fun processQueue() {
        try {
            while (true) {
                val batch =
                    queueMutex.withLock {
                        if (pendingOperations.isEmpty()) {
                            // Observe "empty" and stop processing atomically: if a concurrent queueOperation adds an
                            // item after this, it acquires the mutex next, sees isProcessingQueue == false, and starts
                            // a fresh processor rather than deferring to this dying one.
                            isProcessingQueue = false
                            return@withLock emptyList()
                        }
                        val current = pendingOperations.toList()
                        pendingOperations.clear()
                        publishQueuedOperations()
                        current
                    }
                if (batch.isEmpty()) {
                    break
                }

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

                queueMutex.withLock {
                    pendingOperations.addAll(0, failed)
                    publishQueuedOperations()
                }
                delay(backoffMillis(failed.maxOf { it.retryCount }))
            }
        } finally {
            queueMutex.withLock { isProcessingQueue = false }
        }
    }

    private fun publishQueuedOperations() {
        queuedOperations.value = pendingOperations.toList()
    }

    private fun publishOperationResults() {
        operationResultsState.value = operationResults.toMap()
    }

    private suspend fun recordResult(result: OperationResult) {
        queueMutex.withLock {
            operationResults[result.operationId] = result
            publishOperationResults()
        }
    }

    /**
     * Sends each reference in [operation] to the server and returns the references that failed, so retries only touch
     * the references that did not succeed. References that sync successfully are promoted to remote-synced in the cache
     * so a later server merge does not leave a stale pending row beside the server copy.
     *
     * If the signed-in account has changed since [operation] was queued, it is dropped without sending so that one
     * account's writes can never reach another account.
     */
    private suspend fun processOperation(operation: PendingHighlightOperation): List<BibleReference> {
        if (operation.accountId != currentAccountId()) {
            Logger.w { "Dropping highlight operation ${operation.id} queued under a different account" }
            return emptyList()
        }

        val change = operation.change
        val failedReferences = mutableListOf<BibleReference>()
        for (reference in operation.references) {
            val passageId = "${reference.bookUSFM}.${reference.chapter}.${reference.verseStart ?: 1}"
            val succeeded =
                when (change) {
                    is HighlightChange.Add ->
                        api.createHighlight(reference.versionId, passageId, hexWithoutHash(change.color))
                    is HighlightChange.UpdateColor ->
                        api.updateHighlight(reference.versionId, passageId, hexWithoutHash(change.color))
                    HighlightChange.Remove ->
                        api.deleteHighlight(reference.versionId, passageId)
                }
            if (!succeeded) {
                failedReferences.add(reference)
            }
        }

        if (change !is HighlightChange.Remove) {
            val syncedReferences = operation.references - failedReferences.toSet()
            if (syncedReferences.isNotEmpty()) {
                cache.markHighlightsAsSynced(syncedReferences, notModifiedAfter = operation.timestamp)
            }
        }
        return failedReferences
    }

    private suspend fun loadChapterFromServer(chapter: BibleReference) {
        try {
            val passageId = "${chapter.bookUSFM}.${chapter.chapter}"
            val serverHighlights =
                api
                    .highlights(versionId = chapter.versionId, passageId = passageId)
                    .mapNotNull { it.bibleHighlight() }
            currentCoroutineContext().ensureActive()
            cache.applyServerHighlights(chapter = chapter, highlights = serverHighlights)
            cache.recordChapterFetch(chapter)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e) { "Failed to load highlights for chapter $chapter" }
        } finally {
            cache.unmarkChapterAsLoading(chapter)
        }
    }

    private fun Highlight.bibleHighlight(): BibleHighlight? {
        val reference = BibleReference.unvalidatedReference(usfm = passageId, versionId = versionId)
        if (reference == null) {
            Logger.w { "Ignoring highlight with unparseable passage id: $passageId" }
            return null
        }
        return BibleHighlight(bibleReference = reference, hexColor = hexWithHash(color))
    }

    private fun hexWithHash(color: String): String = if (color.startsWith("#")) color else "#$color"

    private fun hexWithoutHash(color: String): String = color.removePrefix("#").lowercase()

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
