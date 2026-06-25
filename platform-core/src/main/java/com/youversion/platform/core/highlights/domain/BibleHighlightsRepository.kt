package com.youversion.platform.core.highlights.domain

import co.touchlab.kermit.Logger
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.api.HighlightsApi
import com.youversion.platform.core.highlights.api.HighlightsEndpoints
import com.youversion.platform.core.highlights.models.BibleHighlight
import com.youversion.platform.core.highlights.models.Highlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import java.util.UUID
import kotlin.math.pow

/**
 * The type of change a [PendingHighlightOperation] represents.
 */
enum class HighlightOperationType {
    ADD,
    REMOVE,
    UPDATE,
}

/**
 * A highlight change that has been applied to the local cache and is queued to be synced to the server.
 */
data class PendingHighlightOperation(
    val references: List<BibleReference>,
    val color: String?,
    val operationType: HighlightOperationType,
    val id: UUID = UUID.randomUUID(),
    val timestamp: Date = Date(),
    val retryCount: Int = 0,
)

/**
 * Coordinates Bible highlights between the local [BibleHighlightCache] and the YouVersion highlights API.
 *
 * Reads are served from the observable cache and refreshed per-chapter from the server (throttled). Writes update
 * the cache immediately (optimistically) and are queued for the server with retry/backoff so they survive transient
 * failures and offline periods.
 */
class BibleHighlightsRepository(
    private val api: HighlightsApi = HighlightsEndpoints,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val cache = BibleHighlightCache

    private val pendingOperations = mutableListOf<PendingHighlightOperation>()
    private val queueMutex = Mutex()
    private var isProcessingQueue = false

    /**
     * The observable list of cached highlights. UI layers should collect this and filter to the references they render.
     */
    val highlights: StateFlow<List<BibleHighlightCache.CachedHighlight>>
        get() = cache.highlights

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

        scope.launch { loadChapterFromServer(chapter) }
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
                color = color,
                operationType = HighlightOperationType.ADD,
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
                color = null,
                operationType = HighlightOperationType.REMOVE,
            ),
        )
    }

    /**
     * Recolors the highlights on each of [references] to [newColor], updating the cache immediately and syncing to the
     * server.
     */
    fun updateHighlightColors(
        references: List<BibleReference>,
        newColor: String,
    ) {
        cache.updateHighlightColors(references, newColor)
        queueOperation(
            PendingHighlightOperation(
                references = references,
                color = newColor,
                operationType = HighlightOperationType.UPDATE,
            ),
        )
    }

    /**
     * Clears all cached highlights and load state, and discards any pending or in-flight sync operations. Call this
     * when the user signs out so queued writes never land on the previous user's account.
     */
    fun reset() {
        scope.coroutineContext.cancelChildren()
        cache.clear()
        scope.launch {
            queueMutex.withLock {
                pendingOperations.clear()
                isProcessingQueue = false
            }
        }
    }

    private fun queueOperation(operation: PendingHighlightOperation) {
        scope.launch {
            queueMutex.withLock {
                pendingOperations.add(operation)
                pendingOperations.sortBy { it.timestamp }
            }
            processQueue()
        }
    }

    private suspend fun processQueue() {
        queueMutex.withLock {
            if (isProcessingQueue) {
                return
            }
            isProcessingQueue = true
        }

        try {
            while (true) {
                val batch =
                    queueMutex.withLock {
                        val current = pendingOperations.toList()
                        pendingOperations.clear()
                        current
                    }
                if (batch.isEmpty()) {
                    break
                }

                val failed = mutableListOf<PendingHighlightOperation>()
                for (operation in batch) {
                    val failedReferences =
                        try {
                            processOperation(operation)
                        } catch (e: Exception) {
                            Logger.e(e) { "Highlight operation ${operation.id} threw" }
                            operation.references
                        }
                    if (failedReferences.isNotEmpty()) {
                        failed.add(
                            operation.copy(
                                references = failedReferences,
                                retryCount = operation.retryCount + 1,
                            ),
                        )
                    }
                }

                if (failed.isEmpty()) {
                    continue
                }

                val (toRetry, exhausted) = failed.partition { it.retryCount <= MAX_RETRY_COUNT }
                if (exhausted.isNotEmpty()) {
                    Logger.w {
                        "Dropping ${exhausted.size} highlight operation(s) after $MAX_RETRY_COUNT failed retries"
                    }
                }
                if (toRetry.isEmpty()) {
                    continue
                }

                queueMutex.withLock { pendingOperations.addAll(0, toRetry) }
                delay(backoffMillis(toRetry.maxOf { it.retryCount }))
            }
        } finally {
            queueMutex.withLock { isProcessingQueue = false }
        }
    }

    /**
     * Sends each reference in [operation] to the server and returns the references that failed, so retries only touch
     * the references that did not succeed.
     */
    private suspend fun processOperation(operation: PendingHighlightOperation): List<BibleReference> {
        val failedReferences = mutableListOf<BibleReference>()
        for (reference in operation.references) {
            val passageId = "${reference.bookUSFM}.${reference.chapter}.${reference.verseStart ?: 1}"
            val succeeded =
                when (operation.operationType) {
                    HighlightOperationType.ADD ->
                        api.createHighlight(reference.versionId, passageId, hexWithoutHash(operation.color))
                    HighlightOperationType.UPDATE ->
                        api.updateHighlight(reference.versionId, passageId, hexWithoutHash(operation.color))
                    HighlightOperationType.REMOVE ->
                        api.deleteHighlight(reference.versionId, passageId)
                }
            if (!succeeded) {
                failedReferences.add(reference)
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
            cache.applyServerHighlights(chapter = chapter, highlights = serverHighlights)
            cache.recordChapterFetch(chapter)
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

    private fun hexWithoutHash(color: String?): String = color?.removePrefix("#")?.lowercase() ?: ""

    private fun backoffMillis(retryCount: Int): Long {
        val seconds = 2.0.pow(minOf(retryCount, MAX_BACKOFF_EXPONENT)).toLong()
        return minOf(seconds * MILLIS_PER_SECOND, MAX_BACKOFF_MILLIS)
    }

    private companion object {
        const val MAX_RETRY_COUNT = 5
        const val MAX_BACKOFF_EXPONENT = 5
        const val MILLIS_PER_SECOND = 1_000L
        const val MAX_BACKOFF_MILLIS = 30_000L
    }
}
