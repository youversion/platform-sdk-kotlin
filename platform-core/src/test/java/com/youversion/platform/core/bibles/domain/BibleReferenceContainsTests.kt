package com.youversion.platform.core.bibles.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleReferenceContainsTests {
    @Test
    fun `test contains same reference`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)

        assertTrue(ref1.contains(ref2))
        assertTrue(ref2.contains(ref1))
    }

    @Test
    fun `test contains range contains single verse`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 5)

        assertTrue(ref1.contains(ref2))
        assertFalse(ref2.contains(ref1))
    }

    @Test
    fun `test contains range contains smaller range`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 10)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 7)

        assertTrue(ref1.contains(ref2))
        assertFalse(ref2.contains(ref1))
    }

    @Test
    fun `test contains range contains range at boundary`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 10)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)

        assertTrue(ref1.contains(ref2))
        assertFalse(ref2.contains(ref1))
    }

    @Test
    fun `test contains range contains range at end boundary`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 10)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 7, verseEnd = 10)

        assertTrue(ref1.contains(ref2))
        assertFalse(ref2.contains(ref1))
    }

    @Test
    fun `test contains overlapping ranges do not contain`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 9)

        assertFalse(ref1.contains(ref2))
        assertFalse(ref2.contains(ref1))
    }

    @Test
    fun `test contains adjacent ranges do not contain`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 5)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 6, verseEnd = 8)

        assertFalse(ref1.contains(ref2))
        assertFalse(ref2.contains(ref1))
    }

    @Test
    fun `test contains separate ranges do not contain`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 5)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 8, verseEnd = 10)

        assertFalse(ref1.contains(ref2))
        assertFalse(ref2.contains(ref1))
    }

    @Test
    fun `test contains different chapters do not contain`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 50)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verseStart = 1, verseEnd = 10)

        assertFalse(ref1.contains(ref2))
        assertFalse(ref2.contains(ref1))
    }

    @Test
    fun `test contains different books do not contain`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 50)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 1, verseStart = 1, verseEnd = 10)

        assertFalse(ref1.contains(ref2))
        assertFalse(ref2.contains(ref1))
    }

    @Test
    fun `test contains different versions do not contain`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 50)
        val ref2 = BibleReference(versionId = 2, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 10)

        assertFalse(ref1.contains(ref2))
        assertFalse(ref2.contains(ref1))
    }

    @Test
    fun `test contains edge case single verse ranges`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 5)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 5)

        assertTrue(ref1.contains(ref2))
        assertTrue(ref2.contains(ref1))
    }

    @Test
    fun `test contains edge case large ranges`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "PSA", chapter = 119, verseStart = 1, verseEnd = 176)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "PSA", chapter = 119, verseStart = 80, verseEnd = 120)

        assertTrue(ref1.contains(ref2))
        assertFalse(ref2.contains(ref1))
    }

    @Test
    fun `test contains edge case nil handling`() {
        // Test edge cases around nil handling in the contains function
        // The function defaults nil verseStart to 1 and nil verseEnd to verseStart
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 5)

        assertFalse(ref1.contains(ref2))
        assertTrue(ref2.contains(ref1))
    }

    @Test
    fun `test contains chapter contains single verse`() {
        val chapter = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val single = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 10)

        assertTrue(chapter.contains(single))
        assertFalse(single.contains(chapter))
    }

    @Test
    fun `test contains chapter contains range`() {
        val chapter = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val range = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)

        assertTrue(chapter.contains(range))
        assertFalse(range.contains(chapter))
    }

    @Test
    fun `test contains chapter contains chapter same chapter`() {
        val chapter1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val chapter2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

        assertTrue(chapter1.contains(chapter2))
        assertTrue(chapter2.contains(chapter1))
    }

    @Test
    fun `test contains chapter does not contain different chapter`() {
        val chapter1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val chapter2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2)

        assertFalse(chapter1.contains(chapter2))
        assertFalse(chapter2.contains(chapter1))
    }

    @Test
    fun `test contains complex containment scenarios`() {
        data class Scenario(
            val ref1: BibleReference,
            val ref2: BibleReference,
            val ref1ContainsRef2: Boolean,
            val ref2ContainsRef1: Boolean,
        )

        // Test various complex containment scenarios
        // (ref1, ref2, ref1ContainsRef2, ref2ContainsRef1)
        val scenarios =
            listOf(
                Scenario(
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 10),
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 7),
                    true,
                    false,
                ),
                Scenario(
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 15),
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 10),
                    false,
                    false,
                ),
                Scenario(
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 5),
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 5),
                    true,
                    true,
                ),
                Scenario(
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 5),
                    BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 5),
                    true,
                    false,
                ),
            )

        for (scenario in scenarios) {
            assertEquals(
                scenario.ref1ContainsRef2,
                scenario.ref1.contains(scenario.ref2),
                "Failed for ref1.contains(ref2): ref1: ${scenario.ref1}, ref2: ${scenario.ref2}",
            )
            assertEquals(
                scenario.ref2ContainsRef1,
                scenario.ref2.contains(scenario.ref1),
                "Failed for ref2.contains(ref1): ref2: ${scenario.ref2}, ref1: ${scenario.ref1}",
            )
        }
    }
}
