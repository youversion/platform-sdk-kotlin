package com.youversion.platform.core.highlights.domain

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.models.BibleHighlight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BibleHighlightsCacheTests {
    private val cache = BibleHighlightCache()

    @Test
    fun `test highlights empty state`() {
        cache.clear()
        assertTrue(
            cache
                .highlights(
                    overlapping =
                        BibleReference(
                            versionId = 1,
                            bookUSFM = "GEN",
                            chapter = 1,
                            verse = 1,
                        ),
                ).isEmpty(),
        )
    }

    // ----- Test Basic CRUD Operations
    @Test
    fun `test highlights add`() {
        cache.clear()
        val highlight =
            BibleHighlight(
                bibleReference =
                    BibleReference(
                        versionId = 1,
                        bookUSFM = "GEN",
                        chapter = 1,
                        verse = 1,
                    ),
                hexColor = "eefeef",
            )

        cache.addHighlights(listOf(highlight))

        val highlights =
            cache.highlights(
                overlapping = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1),
            )

        assertEquals(1, highlights.size)
        assertEquals(1, highlights.first().bibleReference.versionId)
        assertEquals("GEN", highlights.first().bibleReference.bookUSFM)
        assertEquals(1, highlights.first().bibleReference.chapter)
        assertEquals(1, highlights.first().bibleReference.verseStart)
        assertEquals("eefeef", highlights.first().hexColor)
    }

    @Test
    fun `test highlights remove`() {
        cache.clear()
        val highlight =
            BibleHighlight(
                bibleReference =
                    BibleReference(
                        versionId = 1,
                        bookUSFM = "GEN",
                        chapter = 1,
                        verse = 1,
                    ),
                hexColor = "eefeef",
            )

        cache.addHighlights(listOf(highlight))
        cache.removeHighlights(
            listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
        )

        val highlights =
            cache.highlights(
                overlapping = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1),
            )

        assertTrue(highlights.isEmpty())
    }

    @Test
    fun `test highlights update colors`() {
        cache.clear()
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val highlight = BibleHighlight(bibleReference = ref, hexColor = "eefeef")

        cache.addHighlights(listOf(highlight))
        cache.updateHighlightColors(listOf(ref), newColor = "0000ff")

        val highlights = cache.highlights(overlapping = ref)

        assertEquals(1, highlights.size)
        assertEquals("0000ff", highlights.first().hexColor)
    }

    // ----- Test Range Queries
    @Test
    fun `test highlights get range`() {
        cache.clear()
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 2)
        val ref3 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)
        val highlight1 = BibleHighlight(bibleReference = ref1, hexColor = "eefeef")
        val highlight2 = BibleHighlight(bibleReference = ref2, hexColor = "0000ff")
        val highlight3 = BibleHighlight(bibleReference = ref3, hexColor = "00ffff")

        cache.addHighlights(listOf(highlight1, highlight2, highlight3))

        val highlights =
            cache.highlights(
                overlapping =
                    BibleReference(
                        versionId = 1,
                        bookUSFM = "GEN",
                        chapter = 1,
                        verseStart = 1,
                        verseEnd = 3,
                    ),
            )

        assertEquals(3, highlights.size)
        assertTrue(highlights.any { it.bibleReference.verseStart == 1 })
        assertTrue(highlights.any { it.bibleReference.verseStart == 2 })
        assertTrue(highlights.any { it.bibleReference.verseStart == 3 })
    }

    @Test
    fun `test highlights get cross chapter`() {
        cache.clear()
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 1)
        val highlight1 = BibleHighlight(bibleReference = ref1, hexColor = "eefeef")
        val highlight2 = BibleHighlight(bibleReference = ref2, hexColor = "0000ff")

        cache.addHighlights(listOf(highlight1, highlight2))

        // Test that we only get highlights from chapter 1 when querying chapter 1
        val highlights =
            cache.highlights(
                overlapping =
                    BibleReference(
                        versionId = 1,
                        bookUSFM = "GEN",
                        chapter = 1,
                        verseStart = 1,
                        verseEnd = 1,
                    ),
            )

        assertEquals(1, highlights.size)
        assertTrue(highlights.any { it.bibleReference.chapter == 1 && it.bibleReference.verseStart == 1 })
        assertFalse(highlights.any { it.bibleReference.chapter == 2 && it.bibleReference.verseStart == 1 })
    }

    // ----- Test Observable State
    @Test
    fun `test highlights state flow emits on mutation`() {
        cache.clear()
        assertTrue(cache.highlights.value.isEmpty())

        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        cache.addHighlights(listOf(BibleHighlight(bibleReference = ref, hexColor = "eefeef")))
        assertEquals(1, cache.highlights.value.size)

        cache.removeHighlights(listOf(ref))
        assertTrue(cache.highlights.value.isEmpty())
    }

    // ----- Test Server Merge
    @Test
    fun `test apply server highlights`() {
        cache.clear()
        val chapter = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val load = assertNotNull(cache.markChapterAsLoading(chapter))
        val server =
            listOf(
                BibleHighlight(
                    bibleReference =
                        BibleReference(
                            versionId = 1,
                            bookUSFM = "GEN",
                            chapter = 1,
                            verse = 1,
                        ),
                    hexColor = "#ff0000",
                ),
            )

        val applied = cache.applyServerHighlights(chapter = chapter, highlights = server, load = load)

        assertTrue(applied)
        val highlights = cache.highlights(overlapping = chapter)
        assertEquals(1, highlights.size)
        assertEquals("#ff0000", highlights.first().hexColor)
    }

    @Test
    fun `apply server highlights skips a load that a clear has deregistered`() {
        val chapter = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val load = assertNotNull(cache.markChapterAsLoading(chapter))
        val server =
            listOf(
                BibleHighlight(
                    bibleReference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1),
                    hexColor = "#ff0000",
                ),
            )

        // A clear (e.g. an account change) deregisters the in-flight load. Its late server response must not repopulate
        // the just-cleared cache, or one account's highlights could surface under the next. The merge reports it did not
        // apply so the caller also skips recording the fetch, which would otherwise throttle the next account's reload.
        cache.clear()
        val applied = cache.applyServerHighlights(chapter = chapter, highlights = server, load = load)

        assertFalse(applied)
        assertTrue(cache.highlights(overlapping = chapter).isEmpty())
        assertTrue(cache.highlights.value.isEmpty())
    }

    // ----- Test Sync Promotion
    @Test
    fun `markHighlightsAsSynced converts a superseded pending create into a pending update`() {
        cache.clear()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        cache.addHighlights(listOf(BibleHighlight(bibleReference = reference, hexColor = "#ff0000")))

        // notModifiedAfter predates the local edit, so the row counts as superseded by a newer pending write: the
        // create reached the server, so its queued write becomes an update rather than a second create.
        cache.markHighlightsAsSynced(listOf(reference), notModifiedAfter = Date(0))

        assertEquals(
            BibleHighlightCache.CachedHighlightState.LOCAL_PENDING_UPDATE,
            cache.highlights.value
                .single()
                .state,
        )
    }

    @Test
    fun `markHighlightsAsSynced promotes an unsuperseded pending create to remote synced`() {
        cache.clear()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        cache.addHighlights(listOf(BibleHighlight(bibleReference = reference, hexColor = "#ff0000")))

        cache.markHighlightsAsSynced(listOf(reference), notModifiedAfter = Date(Long.MAX_VALUE))

        assertEquals(
            BibleHighlightCache.CachedHighlightState.REMOTE_SYNCED,
            cache.highlights.value
                .single()
                .state,
        )
    }

    @Test
    fun `removeHighlights tombstones a reference so it is hidden and not server-backed`() {
        cache.clear()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        cache.addHighlights(listOf(BibleHighlight(bibleReference = reference, hexColor = "#ff0000")))

        cache.removeHighlights(listOf(reference))

        assertTrue(cache.highlights(overlapping = reference).isEmpty())
        assertTrue(cache.highlights.value.isEmpty())
        assertFalse(cache.isHighlightServerBacked(reference))
    }

    @Test
    fun `recoloring a tombstoned reference re-creates it as a pending create`() {
        cache.clear()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        cache.addHighlights(listOf(BibleHighlight(bibleReference = reference, hexColor = "#ff0000")))
        cache.removeHighlights(listOf(reference))

        // A re-highlight after a delete must sync as a create, not an update: the server is about to delete the row, so
        // a PUT would target a resource that no longer exists.
        cache.updateHighlightColors(listOf(reference), newColor = "#00ff00")

        assertEquals(
            BibleHighlightCache.CachedHighlightState.LOCAL_PENDING_CREATE,
            cache.highlights.value
                .single()
                .state,
        )
        assertFalse(cache.isHighlightServerBacked(reference))
    }

    // ----- Test Chapter-Load Await
    @Test
    fun `awaitChapterLoaded returns immediately when no load is in flight`() =
        runTest {
            cache.clear()

            // No load is marked for this chapter, so awaiting must not suspend; reaching the assertion proves it.
            cache.awaitChapterLoaded(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))

            assertFalse(
                cache.isChapterLoading(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)),
            )
        }

    @Test
    fun `awaitChapterLoaded suspends until the chapter load is unmarked`() =
        runTest {
            cache.clear()
            val chapter = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
            val load = assertNotNull(cache.markChapterAsLoading(chapter))

            var hasResumed = false
            val waiter =
                launch {
                    cache.awaitChapterLoaded(chapter)
                    hasResumed = true
                }

            runCurrent()
            assertFalse(hasResumed)

            cache.unmarkChapterAsLoading(chapter, load)
            runCurrent()
            assertTrue(hasResumed)
            waiter.join()
        }
}
