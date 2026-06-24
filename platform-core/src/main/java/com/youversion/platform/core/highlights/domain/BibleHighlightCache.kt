package com.youversion.platform.core.highlights.domain

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.models.BibleHighlight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class BibleHighlightCache {
    // ----- Types
    enum class CachedHighlightState {
        REMOTE_SYNCED,
        LOCAL_PENDING_CREATE,
        LOCAL_PENDING_UPDATE,

        // Tombstone: removed locally, hidden from observers, and never resurrected by a server merge until a load that
        // started after the delete synced confirms it (see CachedHighlight.clearsAfterLoadSequence).
        LOCAL_PENDING_DELETE,
    }

    data class CachedHighlight(
        val id: UUID = UUID.randomUUID(),
        val highlight: BibleHighlight,
        val state: CachedHighlightState,
        val lastModifiedAt: Date = Date(),
        // For a tombstone, the load sequence at which the delete reached the server (null until it has). A load with a
        // strictly greater ChapterLoad.sequence began afterwards, so its response reflects the deletion.
        val clearsAfterLoadSequence: Long? = null,
    )

    internal class ChapterLoad(
        val sequence: Long,
    ) {
        val completion = CompletableDeferred<Unit>()
    }

    // ----- Observable State
    // Flat list scanned linearly per mutation and per chapter merge; N is a user's highlight count, small enough that
    // O(N) copies stay negligible, so it is intentionally not chapter-indexed. Mirrors the Swift cache.
    private val _highlights = MutableStateFlow<List<CachedHighlight>>(emptyList())
    val highlights: StateFlow<List<CachedHighlight>> =
        MappedStateFlow(_highlights) { cached ->
            cached.filterNot { it.state == CachedHighlightState.LOCAL_PENDING_DELETE }
        }

    // ----- Throttling and Loading
    private val recentChapterFetches = ConcurrentHashMap<BibleReference, Date>()
    private val currentlyLoadingChapters = ConcurrentHashMap<BibleReference, ChapterLoad>()
    private val loadSequence = AtomicLong(0)
    private val throttlingInterval: Duration = 5.minutes

    // ----- Public API - State Management
    fun clear() {
        // Deregister loads, then empty the cache, then wake waiters. Deregister-before-empty lets an in-flight
        // applyServerHighlights that resumes mid-clear see itself superseded and skip its stale merge; empty-before-wake
        // lets a write parked in awaitChapterLoaded classify against the emptied cache rather than stale rows.
        val loads = currentlyLoadingChapters.values.toList()
        currentlyLoadingChapters.clear()
        recentChapterFetches.clear()
        _highlights.value = emptyList()
        loads.forEach { it.completion.complete(Unit) }
    }

    // ----- Public API - Queries
    fun highlights(overlapping: BibleReference): List<BibleHighlight> =
        _highlights.value
            .filter { it.state != CachedHighlightState.LOCAL_PENDING_DELETE }
            .filter { it.highlight.bibleReference.overlaps(otherReference = overlapping) }
            .map { it.highlight }

    /**
     * Whether the server is known to hold a highlight for [reference], i.e. the cache has an entry for it that is neither
     * a [CachedHighlightState.LOCAL_PENDING_CREATE] nor a [CachedHighlightState.LOCAL_PENDING_DELETE] tombstone. Callers
     * use this to tell a recolor of a server-backed highlight (which should sync as an update) apart from a recolor of
     * one the server has never seen or one the user has just removed (both of which should sync as a create).
     */
    fun isHighlightServerBacked(reference: BibleReference): Boolean =
        _highlights.value.any {
            it.highlight.bibleReference == reference &&
                it.state != CachedHighlightState.LOCAL_PENDING_CREATE &&
                it.state != CachedHighlightState.LOCAL_PENDING_DELETE
        }

    fun hasRecentlyLoadedChapter(chapter: BibleReference): Boolean {
        val chapterKey = normalizeToChapter(chapter)
        val lastFetch = recentChapterFetches[chapterKey] ?: return false
        return Date().time - lastFetch.time < throttlingInterval.inWholeMilliseconds
    }

    fun isChapterLoading(chapter: BibleReference): Boolean =
        currentlyLoadingChapters.containsKey(normalizeToChapter(chapter))

    fun markChapterAsLoading(chapter: BibleReference): ChapterLoad? {
        val load = ChapterLoad(loadSequence.incrementAndGet())
        return load.takeIf { currentlyLoadingChapters.putIfAbsent(normalizeToChapter(chapter), it) == null }
    }

    /**
     * Ends the load represented by [load], completing its waiters. Only clears the entry if [load] is still the
     * registered load for [chapter]: a [clear] between this load's start and finish can drop the entry and let a newer
     * load register under the same chapter, and completing that newer load's signal here would unpark its
     * [awaitChapterLoaded] waiters before its data has merged.
     */
    fun unmarkChapterAsLoading(
        chapter: BibleReference,
        load: ChapterLoad,
    ) {
        if (currentlyLoadingChapters.remove(normalizeToChapter(chapter), load)) {
            load.completion.complete(Unit)
        }
    }

    /**
     * Suspends until the in-flight load for [chapter] finishes, or returns immediately if no load is in flight. Lets a
     * queued write wait for the chapter's server highlights to arrive before it classifies itself as a create or an
     * update, without polling: the load completes the signal in [unmarkChapterAsLoading].
     */
    suspend fun awaitChapterLoaded(chapter: BibleReference) {
        currentlyLoadingChapters[normalizeToChapter(chapter)]?.completion?.await()
    }

    private fun recordChapterFetch(
        chapter: BibleReference,
        at: Date = Date(),
    ) {
        recentChapterFetches[normalizeToChapter(chapter)] = at
    }

    // ----- Public API - Mutations (write APIs preserved)
    fun addHighlights(highlights: List<BibleHighlight>) {
        _highlights.update { current ->
            current.toMutableList().apply {
                for (highlight in highlights) {
                    // Remove any existing for same exact reference; then append as pending create
                    removeAll { it.highlight.bibleReference == highlight.bibleReference }
                    add(
                        CachedHighlight(highlight = highlight, state = CachedHighlightState.LOCAL_PENDING_CREATE),
                    )
                }
            }
        }
    }

    fun removeHighlights(references: List<BibleReference>) {
        _highlights.update { current ->
            current.toMutableList().apply {
                for (reference in references) {
                    removeAll { it.highlight.bibleReference == reference }
                    add(
                        CachedHighlight(
                            highlight = BibleHighlight(bibleReference = reference, hexColor = ""),
                            state = CachedHighlightState.LOCAL_PENDING_DELETE,
                        ),
                    )
                }
                // Physically remove deletes so the visible list hides them
                removeAll { it.state == CachedHighlightState.LOCAL_PENDING_DELETE }
            }
        }
    }

    fun updateHighlightColors(
        references: List<BibleReference>,
        newColor: String,
    ) {
        _highlights.update { current ->
            current.toMutableList().apply {
                for (reference in references) {
                    val index = indexOfFirst { it.highlight.bibleReference == reference }
                    if (index != -1) {
                        val existing = this[index]
                        val newState =
                            when (existing.state) {
                                CachedHighlightState.LOCAL_PENDING_CREATE,
                                CachedHighlightState.LOCAL_PENDING_DELETE,
                                -> CachedHighlightState.LOCAL_PENDING_CREATE
                                else -> CachedHighlightState.LOCAL_PENDING_UPDATE
                            }
                        this[index] =
                            existing.copy(
                                highlight = BibleHighlight(bibleReference = reference, hexColor = newColor),
                                state = newState,
                                lastModifiedAt = Date(),
                                clearsAfterLoadSequence = null,
                            )
                    } else {
                        // Create if not exists, pending create
                        add(
                            CachedHighlight(
                                highlight = BibleHighlight(bibleReference = reference, hexColor = newColor),
                                state = CachedHighlightState.LOCAL_PENDING_CREATE,
                            ),
                        )
                    }
                }
            }
        }
    }

    // ----- Server Merge Helpers

    /**
     * Merges [highlights] fetched by [load] into the cache and, on success, arms the chapter's reload throttle. Skips
     * the merge when [load] is no longer the registered load for the chapter — a clear or a newer load has superseded
     * it — so a stale response cannot repopulate the cache with a previous account's highlights after a switch. The
     * throttle is likewise armed only while the load is still registered, which narrows but does not fully close the
     * window in which a concurrent clear re-throttles the chapter.
     */
    fun applyServerHighlights(
        chapter: BibleReference,
        highlights: List<BibleHighlight>,
        load: ChapterLoad,
    ) {
        val chapterReference = normalizeToChapter(chapter)
        val thisLoadSequence = load.sequence

        _highlights.update { current ->
            // A superseded load (cleared, or replaced by a newer one) is no longer registered; skip its stale merge so
            // it cannot repopulate the cache. Checked in the update so the CAS retry re-evaluates it against a
            // concurrent clear.
            if (currentlyLoadingChapters[chapterReference] !== load) {
                return@update current
            }
            current.toMutableList().apply {
                // Drop tombstones this load is known to reflect: it started after the delete synced, so the server
                // response already accounts for the removal and can no longer resurrect it. The sequence comes from the
                // load that fetched these highlights, not from whatever load is registered now, so a load superseded by
                // a clear plus a newer load cannot borrow the newer load's sequence and clear a tombstone it predates.
                removeAll { cached ->
                    cached.state == CachedHighlightState.LOCAL_PENDING_DELETE &&
                        isInChapter(cached, chapterReference) &&
                        cached.clearsAfterLoadSequence?.let { thisLoadSequence > it } == true
                }

                // Remove existing remote-synced highlights for this chapter
                removeAll { cached ->
                    cached.state == CachedHighlightState.REMOTE_SYNCED && isInChapter(cached, chapterReference)
                }

                // Append server highlights as remote-synced, but never alongside a still-pending local write or a delete
                // tombstone for the same reference: keep the local entry so its edit is not lost or resurrected, and if
                // it was a pending create, mark it a pending update so the queued write now knows the server holds it.
                for (highlight in highlights) {
                    val localIndex =
                        indexOfFirst {
                            it.highlight.bibleReference == highlight.bibleReference &&
                                it.state != CachedHighlightState.REMOTE_SYNCED
                        }
                    when {
                        localIndex == -1 ->
                            add(
                                CachedHighlight(
                                    highlight = highlight,
                                    state = CachedHighlightState.REMOTE_SYNCED,
                                ),
                            )
                        this[localIndex].state == CachedHighlightState.LOCAL_PENDING_CREATE ->
                            this[localIndex] = this[localIndex].copy(state = CachedHighlightState.LOCAL_PENDING_UPDATE)
                    }
                }
            }
        }
        if (currentlyLoadingChapters[chapterReference] === load) {
            recordChapterFetch(chapter)
        }
    }

    /**
     * Promotes the cached highlights for [references] to [CachedHighlightState.REMOTE_SYNCED] once their write has
     * reached the server. Without this a synced highlight stays pending, and the next [applyServerHighlights] merge
     * leaves the stale pending row alongside the fresh server copy, so the verse appears twice.
     *
     * A row is only promoted to [CachedHighlightState.REMOTE_SYNCED] when it has not been modified after
     * [notModifiedAfter]; a newer local edit keeps its color so it is not overwritten by this older write's completion.
     * A newer edit still sitting in [CachedHighlightState.LOCAL_PENDING_CREATE] is instead moved to
     * [CachedHighlightState.LOCAL_PENDING_UPDATE]: this write reaching the server means the highlight now exists, so the
     * newer edit's queued write must sync as an update rather than firing a second create.
     */
    fun markHighlightsAsSynced(
        references: List<BibleReference>,
        notModifiedAfter: Date,
    ) {
        val referenceSet = references.toSet()
        _highlights.update { current ->
            current.map { cached ->
                when {
                    cached.highlight.bibleReference !in referenceSet ||
                        cached.state == CachedHighlightState.REMOTE_SYNCED ||
                        cached.state == CachedHighlightState.LOCAL_PENDING_DELETE -> cached
                    !cached.lastModifiedAt.after(notModifiedAfter) ->
                        cached.copy(state = CachedHighlightState.REMOTE_SYNCED)
                    cached.state == CachedHighlightState.LOCAL_PENDING_CREATE ->
                        cached.copy(state = CachedHighlightState.LOCAL_PENDING_UPDATE)
                    else -> cached
                }
            }
        }
    }

    /**
     * Records that the delete for each of [references] has reached the server. Their tombstones are kept, not dropped,
     * so a chapter load already in flight — whose response may predate the delete — cannot resurrect the highlight;
     * each tombstone is stamped with the current load sequence so a load that starts afterwards can clear it once the
     * server confirms the removal (see [applyServerHighlights]).
     *
     * A reference re-added since the delete is no longer a tombstone. If its re-add had been promoted to
     * [CachedHighlightState.LOCAL_PENDING_UPDATE] or [CachedHighlightState.REMOTE_SYNCED] (by a synced create or a
     * server merge), it is reset to [CachedHighlightState.LOCAL_PENDING_CREATE]: the server has now deleted the
     * reference, so its pending write must post a new highlight rather than put one the server no longer holds — which
     * would 404 and retry forever.
     */
    fun markDeletesSynced(references: List<BibleReference>) {
        val referenceSet = references.toSet()
        val boundary = loadSequence.get()
        _highlights.update { current ->
            current.map { cached ->
                when {
                    cached.highlight.bibleReference !in referenceSet -> cached
                    cached.state == CachedHighlightState.LOCAL_PENDING_DELETE ->
                        cached.copy(clearsAfterLoadSequence = boundary)
                    cached.state == CachedHighlightState.LOCAL_PENDING_CREATE -> cached
                    else -> cached.copy(state = CachedHighlightState.LOCAL_PENDING_CREATE)
                }
            }
        }
    }

    // ----- Utilities
    private fun isInChapter(
        cached: CachedHighlight,
        chapterReference: BibleReference,
    ): Boolean =
        cached.highlight.bibleReference.bookUSFM == chapterReference.bookUSFM &&
            cached.highlight.bibleReference.chapter == chapterReference.chapter &&
            cached.highlight.bibleReference.versionId == chapterReference.versionId

    private fun normalizeToChapter(reference: BibleReference): BibleReference =
        BibleReference(
            versionId = reference.versionId,
            bookUSFM = reference.bookUSFM,
            chapter = reference.chapter,
        )

    companion object {
        /**
         * The process-wide cache shared by default across [BibleHighlightsRepository] instances, mirroring the Swift
         * SDK's `BibleHighlightsCache.shared`. Inject a separate instance to isolate a repository, e.g. in tests.
         */
        val shared = BibleHighlightCache()
    }
}
