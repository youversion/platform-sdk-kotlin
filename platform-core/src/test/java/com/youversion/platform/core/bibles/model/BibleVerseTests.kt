package com.youversion.platform.core.bibles.model

import com.youversion.platform.core.bibles.models.BibleVerse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BibleVerseTests {
    private fun verse(
        id: String? = "id",
        passageId: String? = "passage",
        title: String? = "title",
    ) = BibleVerse(
        id = id,
        passageId = passageId,
        title = title,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(verse(), verse())
        assertEquals(verse().hashCode(), verse().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            verse(id = null),
            verse(passageId = null),
            verse(title = null),
        ).forEach {
            assertNotEquals(verse(), it)
            assertNotEquals(verse().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("BibleVerse(id=id, passageId=passage, title=title)", verse().toString())
    }
}
