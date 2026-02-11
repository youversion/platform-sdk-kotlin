package com.youversion.platform.ui.views

import com.youversion.platform.core.bibles.domain.BibleReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun `throws when annotation has too few parts`() {
        assertFailsWith<IndexOutOfBoundsException> {
            BibleReference.fromAnnotation("1:GEN:1")
        }
    }

    @Test
    fun `throws when versionId is not an integer`() {
        assertFailsWith<NumberFormatException> {
            BibleReference.fromAnnotation("foo:GEN:1:3")
        }
    }

    @Test
    fun `throws when chapter is not an integer`() {
        assertFailsWith<NumberFormatException> {
            BibleReference.fromAnnotation("1:GEN:baz:3")
        }
    }

    @Test
    fun `throws when verse is not an integer`() {
        assertFailsWith<NumberFormatException> {
            BibleReference.fromAnnotation("1:GEN:1:bang")
        }
    }

    @Test
    fun `throws when annotation is empty`() {
        assertFailsWith<NumberFormatException> {
            BibleReference.fromAnnotation("")
        }
    }
}
