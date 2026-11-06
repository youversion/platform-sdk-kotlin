package com.youversion.platform.core.bibles.model

import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVerse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BibleChapterTests {
    private fun chapter(
        id: String? = "id",
        passageId: String? = "passage",
        title: String? = "title",
        verses: List<BibleVerse>? = emptyList(),
    ) = BibleChapter(
        id = id,
        passageId = passageId,
        title = title,
        verses = verses,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(chapter(), chapter())
        assertEquals(chapter().hashCode(), chapter().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            chapter(id = null),
            chapter(passageId = null),
            chapter(title = null),
            chapter(verses = null),
        ).forEach {
            assertNotEquals(chapter(), it)
            assertNotEquals(chapter().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("BibleChapter(id=id, passageId=passage, title=title, verses=[])", chapter().toString())
    }
}
