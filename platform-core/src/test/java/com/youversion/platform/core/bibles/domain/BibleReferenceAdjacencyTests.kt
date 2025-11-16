package com.youversion.platform.core.bibles.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleReferenceAdjacencyTests {
    @Test
    fun `test adjacent individual verses`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 4)

        assertTrue(ref1.isAdjacentOrOverlapping(ref2))
        assertTrue(ref2.isAdjacentOrOverlapping(ref1)) // order doesn't matter
    }

    @Test
    fun `test adjacent verse ranges not overlapping`() {
        // 1-3 contiguous with 4-6
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 4, verseEnd = 6)

        assertTrue(ref1.isAdjacentOrOverlapping(ref2))
        assertTrue(ref2.isAdjacentOrOverlapping(ref1))
    }

    @Test
    fun `test adjacent verse ranges overlapping`() {
        // 1-3 contiguous with 4-6
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 4)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 6)

        assertTrue(ref1.isAdjacentOrOverlapping(ref2))
        assertTrue(ref2.isAdjacentOrOverlapping(ref1))
    }

    @Test
    fun `test not adjacent different book`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 1, verse = 1)

        assertFalse(ref1.isAdjacentOrOverlapping(ref2))
        assertFalse(ref2.isAdjacentOrOverlapping(ref1))
    }

    @Test
    fun `test not adjacent different chapter`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 1)

        assertFalse(ref1.isAdjacentOrOverlapping(ref2))
        assertFalse(ref2.isAdjacentOrOverlapping(ref1))
    }

    @Test
    fun `test not adjacent verse gap`() {
        // 1-3 not contiguous with 5-6
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 6)

        assertFalse(ref1.isAdjacentOrOverlapping(ref2))
        assertFalse(ref2.isAdjacentOrOverlapping(ref1))
    }
}
