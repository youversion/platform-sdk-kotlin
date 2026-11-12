package com.youversion.platform.core.bibles.model

import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleBookIntro
import com.youversion.platform.core.bibles.models.BibleChapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BibleBookTests {
    private fun book(
        id: String? = "id",
        title: String? = "title",
        fullTitle: String? = "fullTitle",
        abbreviation: String? = "abbreviation",
        canon: String? = "canon",
        chapters: List<BibleChapter>? = emptyList(),
        intro: BibleBookIntro? = null,
    ) = BibleBook(
        id = id,
        title = title,
        fullTitle = fullTitle,
        abbreviation = abbreviation,
        canon = canon,
        chapters = chapters,
        intro = intro,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(book(), book())
        assertEquals(book().hashCode(), book().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            book(id = null),
            book(title = null),
            book(fullTitle = null),
            book(abbreviation = null),
            book(canon = null),
            book(chapters = null),
            book(intro = BibleBookIntro("id", "passage", "title")),
        ).forEach {
            assertNotEquals(book(), it)
            assertNotEquals(book().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals(
            "BibleBook(id=id, title=title, fullTitle=fullTitle, abbreviation=abbreviation, " +
                "canon=canon, chapters=[], intro=null)",
            book().toString(),
        )
    }
}
