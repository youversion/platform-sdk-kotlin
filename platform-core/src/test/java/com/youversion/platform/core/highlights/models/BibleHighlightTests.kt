package com.youversion.platform.core.highlights.models

import com.youversion.platform.core.bibles.domain.BibleReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BibleHighlightTests {
    private val reference = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 3)

    private fun highlight(
        bibleReference: BibleReference = reference,
        hexColor: String = "color",
    ) = BibleHighlight(
        bibleReference = bibleReference,
        hexColor = hexColor,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(highlight(), highlight())
        assertEquals(highlight().hashCode(), highlight().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            highlight(bibleReference = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 4)),
            highlight(hexColor = "other"),
        ).forEach {
            assertNotEquals(highlight(), it)
            assertNotEquals(highlight().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("$reference : color", highlight().toString())
    }

    @Test
    fun `test highlight colors match by red, green and blue`() {
        assertTrue(isSameHighlightColor("#80DFDCFF", "dfdcff"))
        assertTrue(isSameHighlightColor("#abc", "AABBCC"))
        assertTrue(isSameHighlightColor("ffdfdcff", "#80dfdcff"))
        assertTrue(isSameHighlightColor("not-hex", "#NOT-HEX"))

        assertFalse(isSameHighlightColor("#80dfdcff", "80dfdc"))
        assertFalse(isSameHighlightColor("#abc", "abcabc"))
        assertFalse(isSameHighlightColor("dfdcff", "dfdcfe"))
        assertFalse(isSameHighlightColor("dfdcf", "dfdcf0"))
        assertFalse(isSameHighlightColor("+fdcff", "0fdcff"))
    }
}
