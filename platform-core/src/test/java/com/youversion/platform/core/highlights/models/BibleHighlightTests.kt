package com.youversion.platform.core.highlights.models

import com.youversion.platform.core.bibles.domain.BibleReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
}
