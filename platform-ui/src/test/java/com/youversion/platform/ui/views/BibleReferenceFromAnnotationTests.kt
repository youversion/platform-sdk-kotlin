package com.youversion.platform.ui.views

import com.youversion.platform.core.bibles.domain.BibleReference
import kotlin.test.Test
import kotlin.test.assertEquals

class BibleReferenceFromAnnotationTests {
    @Test
    fun `parses standard annotation format`() {
        val result = BibleReference.fromAnnotation("1:GEN:1:3")

        assertEquals(1, result.versionId)
        assertEquals("GEN", result.bookUSFM)
        assertEquals(1, result.chapter)
        assertEquals(3, result.verseStart)
        assertEquals(3, result.verseEnd)
    }

    @Test
    fun `parses annotation with large version id`() {
        val result = BibleReference.fromAnnotation("1588:PSA:119:176")

        assertEquals(1588, result.versionId)
        assertEquals("PSA", result.bookUSFM)
        assertEquals(119, result.chapter)
        assertEquals(176, result.verseStart)
    }

    @Test
    fun `parses annotation with lowercase book`() {
        val result = BibleReference.fromAnnotation("1:gen:1:1")

        assertEquals("gen", result.bookUSFM)
    }

    @Test
    fun `parsed reference overlaps matching selected verse`() {
        val parsed = BibleReference.fromAnnotation("1:GEN:1:5")
        val selected = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 5)

        assertEquals(true, parsed.overlaps(selected))
    }

    @Test
    fun `parsed reference does not overlap different verse`() {
        val parsed = BibleReference.fromAnnotation("1:GEN:1:5")
        val selected = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 6)

        assertEquals(false, parsed.overlaps(selected))
    }

    @Test
    fun `parsed reference overlaps verse range containing it`() {
        val parsed = BibleReference.fromAnnotation("1:GEN:1:5")
        val selected =
            BibleReference(
                versionId = 1,
                bookUSFM = "GEN",
                chapter = 1,
                verseStart = 3,
                verseEnd = 7,
            )

        assertEquals(true, parsed.overlaps(selected))
    }
}
