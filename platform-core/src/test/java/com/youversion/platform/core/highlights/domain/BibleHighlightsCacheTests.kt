package com.youversion.platform.core.highlights.domain

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.models.BibleHighlight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
}
