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
    @Test
    fun `test highlights empty state`() {
        BibleHighlightCache.clear()
        assertTrue(
            BibleHighlightCache
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
        BibleHighlightCache.clear()
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

        BibleHighlightCache.addHighlights(listOf(highlight))

        val highlights =
            BibleHighlightCache.highlights(
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
        BibleHighlightCache.clear()
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

        BibleHighlightCache.addHighlights(listOf(highlight))
        BibleHighlightCache.removeHighlights(
            listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
        )

        val highlights =
            BibleHighlightCache.highlights(
                overlapping = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1),
            )

        assertTrue(highlights.isEmpty())
    }

    @Test
    fun `test highlights update colors`() {
        BibleHighlightCache.clear()
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val highlight = BibleHighlight(bibleReference = ref, hexColor = "eefeef")

        BibleHighlightCache.addHighlights(listOf(highlight))
        BibleHighlightCache.updateHighlightColors(listOf(ref), newColor = "0000ff")

        val highlights = BibleHighlightCache.highlights(overlapping = ref)

        assertEquals(1, highlights.size)
        assertEquals("0000ff", highlights.first().hexColor)
    }

    // ----- Test Range Queries
    @Test
    fun `test highlights get range`() {
        BibleHighlightCache.clear()
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 2)
        val ref3 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)
        val highlight1 = BibleHighlight(bibleReference = ref1, hexColor = "eefeef")
        val highlight2 = BibleHighlight(bibleReference = ref2, hexColor = "0000ff")
        val highlight3 = BibleHighlight(bibleReference = ref3, hexColor = "00ffff")

        BibleHighlightCache.addHighlights(listOf(highlight1, highlight2, highlight3))

        val highlights =
            BibleHighlightCache.highlights(
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
        BibleHighlightCache.clear()
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 1)
        val highlight1 = BibleHighlight(bibleReference = ref1, hexColor = "eefeef")
        val highlight2 = BibleHighlight(bibleReference = ref2, hexColor = "0000ff")

        BibleHighlightCache.addHighlights(listOf(highlight1, highlight2))

        // Test that we only get highlights from chapter 1 when querying chapter 1
        val highlights =
            BibleHighlightCache.highlights(
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
        BibleHighlightCache.clear()
        assertTrue(BibleHighlightCache.highlights.value.isEmpty())

        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        BibleHighlightCache.addHighlights(listOf(BibleHighlight(bibleReference = ref, hexColor = "eefeef")))
        assertEquals(1, BibleHighlightCache.highlights.value.size)

        BibleHighlightCache.removeHighlights(listOf(ref))
        assertTrue(BibleHighlightCache.highlights.value.isEmpty())
    }

    // ----- Test Server Merge
    @Test
    fun `test apply server highlights`() {
        BibleHighlightCache.clear()
        val chapter = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
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

        BibleHighlightCache.applyServerHighlights(chapter = chapter, highlights = server)

        val highlights = BibleHighlightCache.highlights(overlapping = chapter)
        assertEquals(1, highlights.size)
        assertEquals("#ff0000", highlights.first().hexColor)
    }

    // ----- Test Sync Promotion
    @Test
    fun `markHighlightsAsSynced converts a superseded pending create into a pending update`() {
        BibleHighlightCache.clear()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        BibleHighlightCache.addHighlights(listOf(BibleHighlight(bibleReference = reference, hexColor = "#ff0000")))

        // notModifiedAfter predates the local edit, so the row counts as superseded by a newer pending write: the
        // create reached the server, so its queued write becomes an update rather than a second create.
        BibleHighlightCache.markHighlightsAsSynced(listOf(reference), notModifiedAfter = Date(0))

        assertEquals(
            BibleHighlightCache.CachedHighlightState.LOCAL_PENDING_UPDATE,
            BibleHighlightCache.highlights.value
                .single()
                .state,
        )
    }

    @Test
    fun `markHighlightsAsSynced promotes an unsuperseded pending create to remote synced`() {
        BibleHighlightCache.clear()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        BibleHighlightCache.addHighlights(listOf(BibleHighlight(bibleReference = reference, hexColor = "#ff0000")))

        BibleHighlightCache.markHighlightsAsSynced(listOf(reference), notModifiedAfter = Date(Long.MAX_VALUE))

        assertEquals(
            BibleHighlightCache.CachedHighlightState.REMOTE_SYNCED,
            BibleHighlightCache.highlights.value
                .single()
                .state,
        )
    }

    @Test
    fun `removeHighlights tombstones a reference so it is hidden and not server-backed`() {
        BibleHighlightCache.clear()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        BibleHighlightCache.addHighlights(listOf(BibleHighlight(bibleReference = reference, hexColor = "#ff0000")))

        BibleHighlightCache.removeHighlights(listOf(reference))

        assertTrue(BibleHighlightCache.highlights(overlapping = reference).isEmpty())
        assertTrue(BibleHighlightCache.highlights.value.isEmpty())
        assertFalse(BibleHighlightCache.isHighlightServerBacked(reference))
    }

    @Test
    fun `recoloring a tombstoned reference re-creates it as a pending create`() {
        BibleHighlightCache.clear()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        BibleHighlightCache.addHighlights(listOf(BibleHighlight(bibleReference = reference, hexColor = "#ff0000")))
        BibleHighlightCache.removeHighlights(listOf(reference))

        // A re-highlight after a delete must sync as a create, not an update: the server is about to delete the row, so
        // a PUT would target a resource that no longer exists.
        BibleHighlightCache.updateHighlightColors(listOf(reference), newColor = "#00ff00")

        assertEquals(
            BibleHighlightCache.CachedHighlightState.LOCAL_PENDING_CREATE,
            BibleHighlightCache.highlights.value
                .single()
                .state,
        )
        assertFalse(BibleHighlightCache.isHighlightServerBacked(reference))
    }

    // ----- Test Chapter-Load Await
    @Test
    fun `awaitChapterLoaded returns immediately when no load is in flight`() =
        runTest {
            BibleHighlightCache.clear()

            // No load is marked for this chapter, so awaiting must not suspend; reaching the assertion proves it.
            BibleHighlightCache.awaitChapterLoaded(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))

            assertFalse(
                BibleHighlightCache.isChapterLoading(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)),
            )
        }

    @Test
    fun `awaitChapterLoaded suspends until the chapter load is unmarked`() =
        runTest {
            BibleHighlightCache.clear()
            val chapter = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
            val load = assertNotNull(BibleHighlightCache.markChapterAsLoading(chapter))

            var didResume = false
            val waiter =
                launch {
                    BibleHighlightCache.awaitChapterLoaded(chapter)
                    didResume = true
                }

            runCurrent()
            assertFalse(didResume)

            BibleHighlightCache.unmarkChapterAsLoading(chapter, load)
            runCurrent()
            assertTrue(didResume)
            waiter.join()
        }
}
