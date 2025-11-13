package com.youversion.platform.core.highlights.domain

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.models.BibleHighlight
import java.util.Date
import java.util.UUID
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
    private val cachedHighlights = mutableListOf<CachedHighlight>()
    val highlights: List<CachedHighlight>
        get() = cachedHighlights.toList()

    // ----- Throttling and Loading
    private var recentChapterFetches = mutableMapOf<BibleReference, Date>()
    private var currentlyLoadingChapters = mutableSetOf<BibleReference>()
    private val throttlingInterval: Duration = 5.minutes

    // ----- Public API - State Management
    fun clear() {
        cachedHighlights.clear()
        recentChapterFetches.clear()
        currentlyLoadingChapters.clear()
    }

    // ----- Public API - Queries
    fun highlights(overlapping: BibleReference): List<BibleHighlight> =
        cachedHighlights
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
        for (highlight in highlights) {
            // Remove any existing for same exact reference; then append as pending create
            cachedHighlights.removeAll { it.highlight.bibleReference == highlight.bibleReference }
            cachedHighlights.add(
                CachedHighlight(highlight = highlight, state = CachedHighlightState.LOCAL_PENDING_CREATE),
            )
        }
    }

    fun removeHighlights(references: List<BibleReference>) {
        for (reference in references) {
            // If there is a pending create for this reference, just drop it; otherwise mark pending delete
            val pendingCreateIdx =
                cachedHighlights.indexOfFirst {
                    it.highlight.bibleReference == reference && it.state == CachedHighlightState.LOCAL_PENDING_CREATE
                }
            if (pendingCreateIdx != -1) {
                cachedHighlights.removeAt(pendingCreateIdx)
            } else {
                val idx = cachedHighlights.indexOfFirst { it.highlight.bibleReference == reference }
                if (idx != -1) {
                    cachedHighlights[idx] =
                        cachedHighlights[idx].copy(
                            state = CachedHighlightState.LOCAL_PENDING_DELETE,
                            lastModifiedAt = Date(),
                        )
                }
            }
        }
        // Optionally, physically remove localPendingDelete from visible list here if UI should hide deletes
        cachedHighlights.removeAll { it.state == CachedHighlightState.LOCAL_PENDING_DELETE }
    }

    fun updateHighlightColors(
        references: List<BibleReference>,
        newColor: String,
    ) {
        for (reference in references) {
            val idx = cachedHighlights.indexOfFirst { it.highlight.bibleReference == reference }
            if (idx != -1) {
                cachedHighlights[idx] =
                    cachedHighlights[idx].copy(
                        highlight = BibleHighlight(bibleReference = reference, hexColor = newColor),
                        state =
                            if (cachedHighlights[idx].state != CachedHighlightState.LOCAL_PENDING_CREATE) {
                                CachedHighlightState.LOCAL_PENDING_UPDATE
                            } else {
                                cachedHighlights[idx].state
                            },
                        lastModifiedAt = Date(),
                    )
            } else {
                // Create if not exists, pending create
                cachedHighlights.add(
                    CachedHighlight(
                        highlight = BibleHighlight(bibleReference = reference, hexColor = newColor),
                        state = CachedHighlightState.LOCAL_PENDING_CREATE,
                    ),
                )
            }
        }
    }

    // ----- Server Merge Helpers
    fun applyServerHighlights(
        chapter: BibleReference,
        highlights: List<BibleHighlight>,
    ) {
        val chapterRef = normalizeToChapter(chapter)

        // Remove existing remote-synced highlights for this chapter
        cachedHighlights.removeAll { ch ->
            ch.state == CachedHighlightState.REMOTE_SYNCED &&
                ch.highlight.bibleReference.bookUSFM == chapterRef.bookUSFM &&
                ch.highlight.bibleReference.chapter == chapterRef.chapter &&
                ch.highlight.bibleReference.versionId == chapterRef.versionId
        }

        // Append server highlights as remoteSynced
        for (h in highlights) {
            cachedHighlights.add(
                CachedHighlight(
                    highlight = h,
                    state = CachedHighlightState.REMOTE_SYNCED,
                ),
            )
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
