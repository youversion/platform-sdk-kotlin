package com.youversion.platform.core.bibles.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BibleReferenceUnvalidatedReferenceTests {
    @Test
    fun `test unvalidated reference single verse`() {
        val ref = BibleReference.unvalidatedReference("GEN.1.3", versionId = 1)
        assertNotNull(ref)

        val expected = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 3)
        assertEquals(expected, ref)
    }

    @Test
    fun `test unvalidated reference verse range`() {
        val ref = BibleReference.unvalidatedReference("GEN.1.3-5", versionId = 1)
        assertNotNull(ref)

        val expected = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 5)
        assertEquals(expected, ref)
    }

    @Test
    fun `test unvalidated reference full range with chapter`() {
        val ref = BibleReference.unvalidatedReference("GEN.1.3-1.5", versionId = 1)
        assertNotNull(ref)

        val expected = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 5)
        assertEquals(expected, ref)
    }

    @Test
    fun `test unvalidated reference full range with book`() {
        val ref = BibleReference.unvalidatedReference("GEN.1.3-GEN.1.5", versionId = 1)
        assertNotNull(ref)

        val expected = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 5)
        assertEquals(expected, ref)
    }

    @Test
    fun `test unvalidated reference invalid book mismatch`() {
        val ref = BibleReference.unvalidatedReference("GEN.1.3-EXO.1.5", versionId = 1)

        assertNull(ref)
    }

    @Test
    fun `test unvalidated reference invalid verse order`() {
        val ref = BibleReference.unvalidatedReference("GEN.1.5-3", versionId = 1)

        assertNull(ref)
    }

    @Test
    fun `test unvalidated reference chapter only`() {
        val ref = BibleReference.unvalidatedReference("GEN.1", versionId = 1)
        assertNotNull(ref)

        val expected = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 1)
        assertEquals(expected, ref)
    }
}
