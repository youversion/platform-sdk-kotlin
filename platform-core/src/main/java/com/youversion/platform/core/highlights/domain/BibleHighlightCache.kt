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

    fun hasRecentlyLoadedChapter(chapter: BibleReference): Boolean {
        val chapterKey = normalizeToChapter(chapter)
        val lastFetch = recentChapterFetches[chapterKey] ?: return false
        return Date().time - lastFetch.time < throttlingInterval.inWholeMilliseconds
    }

    fun isChapterLoading(chapter: BibleReference): Boolean =
        currentlyLoadingChapters.contains(normalizeToChapter(chapter))

    fun markChapterAsLoading(chapter: BibleReference): Boolean {
        val normalized = normalizeToChapter(chapter)
        if (currentlyLoadingChapters.contains(normalized)) {
            return false
        }
        currentlyLoadingChapters.add(normalized)
        return true
    }

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

                // Append server highlights as remoteSynced
                for (highlight in highlights) {
                    add(
                        CachedHighlight(
                            highlight = highlight,
                            state = CachedHighlightState.REMOTE_SYNCED,
                        ),
                    )
                }
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
