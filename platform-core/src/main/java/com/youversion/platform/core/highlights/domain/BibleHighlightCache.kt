package com.youversion.platform.core.highlights.domain

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.models.BibleHighlight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object BibleHighlightCache {
    // ----- Types
    enum class CachedHighlightState {
        REMOTE_SYNCED,
        LOCAL_PENDING_CREATE,
        LOCAL_PENDING_UPDATE,
        LOCAL_PENDING_DELETE,
    }

    data class CachedHighlight(
        val id: UUID = UUID.randomUUID(),
        val highlight: BibleHighlight,
        val state: CachedHighlightState,
        val lastModifiedAt: Date = Date(),
    )

    // ----- Observable State
    private val _highlights = MutableStateFlow<List<CachedHighlight>>(emptyList())
    val highlights: StateFlow<List<CachedHighlight>> = _highlights.asStateFlow()

    // ----- Throttling and Loading
    private val recentChapterFetches = ConcurrentHashMap<BibleReference, Date>()
    private val currentlyLoadingChapters = ConcurrentHashMap.newKeySet<BibleReference>()
    private val throttlingInterval: Duration = 5.minutes

    // ----- Public API - State Management
    fun clear() {
        _highlights.value = emptyList()
        recentChapterFetches.clear()
        currentlyLoadingChapters.clear()
    }

    // ----- Public API - Queries
    fun highlights(overlapping: BibleReference): List<BibleHighlight> =
        _highlights.value
            .filter { it.highlight.bibleReference.overlaps(otherReference = overlapping) }
            .map { it.highlight }

    /**
     * Whether the server is known to hold a highlight for [reference], i.e. the cache has an entry for it in any state
     * other than [CachedHighlightState.LOCAL_PENDING_CREATE]. Callers use this to tell a recolor of a server-backed
     * highlight (which should sync as an update) apart from a recolor of one the server has never seen (which should
     * sync as a create).
     */
    fun isHighlightServerBacked(reference: BibleReference): Boolean =
        _highlights.value.any {
            it.highlight.bibleReference == reference && it.state != CachedHighlightState.LOCAL_PENDING_CREATE
        }

    fun hasRecentlyLoadedChapter(chapter: BibleReference): Boolean {
        val chapterKey = normalizeToChapter(chapter)
        val lastFetch = recentChapterFetches[chapterKey] ?: return false
        return Date().time - lastFetch.time < throttlingInterval.inWholeMilliseconds
    }

    fun isChapterLoading(chapter: BibleReference): Boolean =
        currentlyLoadingChapters.contains(normalizeToChapter(chapter))

    fun markChapterAsLoading(chapter: BibleReference): Boolean =
        currentlyLoadingChapters.add(normalizeToChapter(chapter))

    fun unmarkChapterAsLoading(chapter: BibleReference) {
        currentlyLoadingChapters.remove(normalizeToChapter(chapter))
    }

    fun recordChapterFetch(
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
                    // If there is a pending create for this reference, just drop it; otherwise mark pending delete
                    val pendingCreateIndex =
                        indexOfFirst {
                            it.highlight.bibleReference == reference &&
                                it.state == CachedHighlightState.LOCAL_PENDING_CREATE
                        }
                    if (pendingCreateIndex != -1) {
                        removeAt(pendingCreateIndex)
                    } else {
                        val index = indexOfFirst { it.highlight.bibleReference == reference }
                        if (index != -1) {
                            this[index] =
                                this[index].copy(
                                    state = CachedHighlightState.LOCAL_PENDING_DELETE,
                                    lastModifiedAt = Date(),
                                )
                        }
                    }
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
                        this[index] =
                            this[index].copy(
                                highlight = BibleHighlight(bibleReference = reference, hexColor = newColor),
                                state =
                                    if (this[index].state != CachedHighlightState.LOCAL_PENDING_CREATE) {
                                        CachedHighlightState.LOCAL_PENDING_UPDATE
                                    } else {
                                        this[index].state
                                    },
                                lastModifiedAt = Date(),
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
    fun applyServerHighlights(
        chapter: BibleReference,
        highlights: List<BibleHighlight>,
    ) {
        val chapterRef = normalizeToChapter(chapter)

        _highlights.update { current ->
            current.toMutableList().apply {
                // Remove existing remote-synced highlights for this chapter
                removeAll { cached ->
                    cached.state == CachedHighlightState.REMOTE_SYNCED &&
                        cached.highlight.bibleReference.bookUSFM == chapterRef.bookUSFM &&
                        cached.highlight.bibleReference.chapter == chapterRef.chapter &&
                        cached.highlight.bibleReference.versionId == chapterRef.versionId
                }

                // Append server highlights as remote-synced, but never alongside a still-pending local write for the
                // same reference: keep the optimistic local entry so its edit is not lost, and if it was a pending
                // create, mark it a pending update so the queued write now knows the server holds the highlight.
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
                        cached.state == CachedHighlightState.REMOTE_SYNCED -> cached
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
     * Drops any remote-synced cached highlight for each of [references] once their deletion has reached the server. A
     * delete removes the local entry immediately, so this only matters when a concurrent chapter load re-added the
     * highlight as remote-synced before the delete synced; without it that stale row would linger until the next
     * reload. Local-pending entries are left in place so a re-add made after the delete is not lost.
     */
    fun removeSyncedHighlights(references: List<BibleReference>) {
        val referenceSet = references.toSet()
        _highlights.update { current ->
            current.filterNot {
                it.highlight.bibleReference in referenceSet && it.state == CachedHighlightState.REMOTE_SYNCED
            }
        }
    }

    // ----- Utilities
    private fun normalizeToChapter(reference: BibleReference): BibleReference =
        BibleReference(
            versionId = reference.versionId,
            bookUSFM = reference.bookUSFM,
            chapter = reference.chapter,
        )
}
