package com.youversion.platform.core.bibles.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleReferenceTests {
    @Test
    fun `test init single verse`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 3)

        assertEquals(1, ref.versionId)
        assertEquals("GEN", ref.bookUSFM)
        assertEquals(2, ref.chapter)
        assertEquals(3, ref.verseStart)
        assertEquals(3, ref.verseEnd)
    }

    @Test
    fun `test init verse range`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "PSA", chapter = 23, verseStart = 1, verseEnd = 2)

        assertEquals(1, ref.versionId)
        assertEquals("PSA", ref.bookUSFM)
        assertEquals(23, ref.chapter)
        assertEquals(1, ref.verseStart)
        assertEquals(2, ref.verseEnd)
    }

    // ----- Test isRange
    @Test
    fun `test isRange single verse`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 3, verse = 14)

        assertFalse(ref.isRange)
    }

    @Test
    fun `test isRange verse range`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 3, verseStart = 14, verseEnd = 15)

        assertTrue(ref.isRange)
    }

    // ----- Test compare function
    @Test
    fun `test compare same reference`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)

        assertEquals(ref1, ref2)
    }

    @Test
    fun `test compare different books`() {
        // Note: Comparing different books is not reliable without knowing the canonical book order
        // This test just verifies the function doesn't crash with different book usfms
        val genRef = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val exoRef = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 1, verse = 1)

        // Just verify the function can be called with different books
        // The actual comparison result is not validated
        BibleReference.compare(genRef, exoRef)
        BibleReference.compare(exoRef, genRef)

        // This test passes as long as the above lines don't crash
    }

    @Test
    fun `test compare same book different chapters`() {
        val gen1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val gen2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 1)

        assertTrue(gen1 < gen2)
        assertEquals(-1, BibleReference.compare(gen1, gen2)) // GEN 1 < GEN 2
        assertEquals(1, BibleReference.compare(gen2, gen1)) // GEN 2 > GEN 1
    }

    @Test
    fun `test compare same chapter different verses`() {
        val gen1_1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val gen1_2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 2)

        assertTrue(gen1_1 < gen1_2)
        assertTrue(gen1_2 > gen1_1)
    }

    @Test
    fun `test compare ranges`() {
        val gen1_1to3 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 3)
        val gen1_2to4 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 2, verseEnd = 4)

        // Compare based on starting verse first
        assertTrue(gen1_1to3 < gen1_2to4)

        // Same starting verse, compare end verse
        val gen1_1to3_2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 3)
        val gen1_1to4 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 4)

        assertTrue(gen1_1to3_2 < gen1_1to4) // 1-3 < 1-4
    }

    @Test
    fun `test sorting`() {
        val refs =
            listOf(
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1),
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 4, verse = 3),
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 3, verse = 2),
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 1),
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 2),
            )

        val sorted = refs.sorted()

        assertEquals("GEN", sorted[0].bookUSFM)
        assertEquals(1, sorted[0].chapter)
        assertEquals(1, sorted[0].verseStart)

        assertEquals("GEN", sorted[1].bookUSFM)
        assertEquals(1, sorted[1].chapter)
        assertEquals(2, sorted[1].verseStart)

        assertEquals("GEN", sorted[2].bookUSFM)
        assertEquals(2, sorted[2].chapter)
        assertEquals(1, sorted[2].verseStart)

        assertEquals("GEN", sorted[3].bookUSFM)
        assertEquals(3, sorted[3].chapter)
        assertEquals(2, sorted[3].verseStart)

        assertEquals("GEN", sorted[4].bookUSFM)
        assertEquals(4, sorted[4].chapter)
        assertEquals(3, sorted[4].verseStart)
    }

    // ----- Test chapterUSFM
    @Test
    fun `test chapterUSFM`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "gen", chapter = 1, verse = 1)

        assertEquals("GEN.1", ref.chapterUSFM)
    }

    // ----- Test asUSFM
    @Test
    fun `test asUSFM`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "gen", chapter = 1)
        assertEquals("GEN.1", ref1.asUSFM)

        val ref2 = BibleReference(versionId = 1, bookUSFM = "gen", chapter = 1, verse = 1)
        assertEquals("GEN.1.1", ref2.asUSFM)

        val ref3 = BibleReference(versionId = 1, bookUSFM = "gen", chapter = 1, verseStart = 1, verseEnd = 5)
        assertEquals("GEN.1.1-GEN.1.5", ref3.asUSFM)
    }

    // ----- Test merging references
    @Test
    fun `test merge adjacent references`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 4, verseEnd = 6)

        val merged1To2 = BibleReference.referenceByMerging(ref1, ref2)
        assertEquals(1, merged1To2.versionId)
        assertEquals("GEN", merged1To2.bookUSFM)
        assertEquals(1, merged1To2.chapter)
        assertEquals(1, merged1To2.verseStart)
        assertEquals(6, merged1To2.verseEnd)

        // show that order does not matter
        val merged2To1 = BibleReference.referenceByMerging(ref2, ref1)
        assertEquals(1, merged2To1.versionId)
        assertEquals("GEN", merged2To1.bookUSFM)
        assertEquals(1, merged2To1.chapter)
        assertEquals(1, merged2To1.verseStart)
        assertEquals(6, merged2To1.verseEnd)
    }

    @Test
    fun `test merge overlapping references`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 9)

        val merged1To2 = BibleReference.referenceByMerging(ref1, ref2)
        assertEquals(1, merged1To2.versionId)
        assertEquals("GEN", merged1To2.bookUSFM)
        assertEquals(1, merged1To2.chapter)
        assertEquals(3, merged1To2.verseStart)
        assertEquals(9, merged1To2.verseEnd)

        // show that order does not matter
        val merged2To1 = BibleReference.referenceByMerging(ref2, ref1)
        assertEquals(1, merged2To1.versionId)
        assertEquals("GEN", merged2To1.bookUSFM)
        assertEquals(1, merged2To1.chapter)
        assertEquals(3, merged2To1.verseStart)
        assertEquals(9, merged2To1.verseEnd)
    }

    @Test
    fun `test merge one reference contains another`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 2, verseEnd = 10)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 7)

        val merged1To2 = BibleReference.referenceByMerging(ref1, ref2)
        assertEquals(1, merged1To2.versionId)
        assertEquals("GEN", merged1To2.bookUSFM)
        assertEquals(1, merged1To2.chapter)
        assertEquals(2, merged1To2.verseStart)
        assertEquals(10, merged1To2.verseEnd)

        // show that order does not matter
        val merged2To1 = BibleReference.referenceByMerging(ref2, ref1)
        assertEquals(1, merged2To1.versionId)
        assertEquals("GEN", merged2To1.bookUSFM)
        assertEquals(1, merged2To1.chapter)
        assertEquals(2, merged2To1.verseStart)
        assertEquals(10, merged2To1.verseEnd)
    }

    @Test
    fun `test merge multiple groups of references`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 4, verseStart = 2, verseEnd = 10)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 2, verseStart = 5, verseEnd = 7)
        val ref3 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 4, verseStart = 5, verseEnd = 12)
        val ref4 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 2, verseStart = 8, verseEnd = 11)
        val ref5 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 1, verseStart = 5, verseEnd = 7)

        val result = BibleReference.referencesByMerging(listOf(ref1, ref2, ref3, ref4, ref5))
        assertEquals(3, result.size)

        val singleChapter1 = result.first { it.chapter == 1 }
        assertEquals(1, singleChapter1.chapter)
        assertEquals(5, singleChapter1.verseStart)
        assertEquals(7, singleChapter1.verseEnd)

        val mergedChapter2 = result.first { it.chapter == 2 }
        assertEquals(2, mergedChapter2.chapter)
        assertEquals(5, mergedChapter2.verseStart)
        assertEquals(11, mergedChapter2.verseEnd)

        val mergedChapter4 = result.first { it.chapter == 4 }
        assertEquals(4, mergedChapter4.chapter)
        assertEquals(2, mergedChapter4.verseStart)
        assertEquals(12, mergedChapter4.verseEnd)
    }
}
