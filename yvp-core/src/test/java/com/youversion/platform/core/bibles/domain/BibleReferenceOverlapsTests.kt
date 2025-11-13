package com.youversion.platform.core.bibles.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleReferenceOverlapsTests {
    @Test
    fun `test overlaps same verse`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps different verses`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 4)

        assertFalse(ref1.overlaps(ref2))
        assertFalse(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps verse range overlaps`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 9)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps verse range no overlap`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 5)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 7, verseEnd = 9)

        assertFalse(ref1.overlaps(ref2))
        assertFalse(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps verse range adjacent`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 5)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 6, verseEnd = 8)

        assertFalse(ref1.overlaps(ref2))
        assertFalse(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps one contains other`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 10)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 7)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps single verse in range`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 5)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps single verse at range boundary`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps single verse outside range`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 8)

        assertFalse(ref1.overlaps(ref2))
        assertFalse(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps different chapters`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 3)

        assertFalse(ref1.overlaps(ref2))
        assertFalse(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps different books`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 1, verse = 3)

        assertFalse(ref1.overlaps(ref2))
        assertFalse(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps different versions`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)
        val ref2 = BibleReference(versionId = 2, bookUSFM = "GEN", chapter = 1, verse = 3)

        assertFalse(ref1.overlaps(ref2))
        assertFalse(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps complex overlap scenarios`() {
        data class Scenario(
            val ref1: BibleReference,
            val ref2: BibleReference,
            val shouldOverlap: Boolean,
        )

        // Test various complex overlap scenarios
        // (ref1, ref2, shouldOverlap)
        val scenarios =
            listOf(
                Scenario(
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 10),
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 15),
                    true,
                ),
                Scenario(
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 15),
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 10),
                    true,
                ),
                Scenario(
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 5),
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 6, verseEnd = 10),
                    false,
                ),
                Scenario(
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 5),
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 10),
                    true,
                ),
            )

        for (scenario in scenarios) {
            assertEquals(
                scenario.shouldOverlap,
                scenario.ref1.overlaps(scenario.ref2),
                "Failed for ref1: ${scenario.ref1}, ref2: ${scenario.ref2}",
            )
            assertEquals(
                scenario.shouldOverlap,
                scenario.ref2.overlaps(scenario.ref1),
                "Failed for ref2: ${scenario.ref2}, ref1: ${scenario.ref1}",
            )
        }
    }

    @Test
    fun `test overlaps ordering dependency`() {
        // Test that the min/max ordering in the overlaps function works correctly
        // This tests cases where the order of comparison might matter
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 10)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 7)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps exact boundary overlap`() {
        // Test cases where references touch exactly at boundaries
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 5)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 10)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps identical ranges`() {
        // Test identical ranges
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps edge case nil handling`() {
        // Test edge cases around nil handling in the overlaps function
        // The function defaults nil verseStart to 1 and nil verseEnd to verseStart
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 5)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps case sensitive book comparison`() {
        // Test that book comparison is case-sensitive (as implemented)
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "gen", chapter = 1, verse = 1)

        assertFalse(ref1.overlaps(ref2))
        assertFalse(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps edge case large ranges`() {
        // Test with very large verse ranges
        val ref1 = BibleReference(versionId = 1, bookUSFM = "PSA", chapter = 119, verseStart = 1, verseEnd = 176)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "PSA", chapter = 119, verseStart = 80, verseEnd = 120)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps edge case minimal overlap`() {
        // Test minimal overlap scenarios
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 100)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 100, verseEnd = 200)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps edge case zero length range`() {
        // Test edge case where a range has zero length (start == end)
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 5)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 5)

        assertTrue(ref1.overlaps(ref2))
        assertTrue(ref2.overlaps(ref1))
    }

    @Test
    fun `test overlaps chapter vs single verse`() {
        // Both verseStart and verseEnd nil means whole chapter reference
        val chapter = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val single = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)

        assertTrue(chapter.overlaps(single))
        assertTrue(single.overlaps(chapter))
    }

    @Test
    fun `test overlaps chapter vs range`() {
        val chapter = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val range = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)

        assertTrue(chapter.overlaps(range))
        assertTrue(range.overlaps(chapter))
    }

    @Test
    fun `test overlaps chapter vs chapter same chapter`() {
        val chapter1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val chapter2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

        assertTrue(chapter1.overlaps(chapter2))
        assertTrue(chapter2.overlaps(chapter1))
    }

    @Test
    fun `test overlaps chapter vs chapter different chapter`() {
        val chapter1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val chapter2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2)

        assertFalse(chapter1.overlaps(chapter2))
        assertFalse(chapter2.overlaps(chapter1))
    }

    @Test
    fun `test overlaps chapter vs chapter verse verse different chapter`() {
        val chapter1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val chapter2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verseStart = 3, verseEnd = 3)

        assertFalse(chapter1.overlaps(chapter2))
        assertFalse(chapter2.overlaps(chapter1))
    }
}
