package com.youversion.platform.core.bibles.model

import com.youversion.platform.core.bibles.models.BiblePassage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BiblePassageTests {
    private fun passage(
        id: String = "id",
        content: String = "content",
        reference: String = "reference",
    ) = BiblePassage(
        id = id,
        content = content,
        reference = reference,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(passage(), passage())
        assertEquals(passage().hashCode(), passage().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            passage(id = "other"),
            passage(content = "other"),
            passage(reference = "other"),
        ).forEach {
            assertNotEquals(passage(), it)
            assertNotEquals(passage().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("BiblePassage(id=id, content=content, reference=reference)", passage().toString())
    }
}
