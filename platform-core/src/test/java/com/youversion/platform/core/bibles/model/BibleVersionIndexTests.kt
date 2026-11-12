package com.youversion.platform.core.bibles.model

import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleVersionIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BibleVersionIndexTests {
    private fun index(
        textDirection: String? = "rtl",
        books: List<BibleBook>? = emptyList(),
    ) = BibleVersionIndex(
        textDirection = textDirection,
        books = books,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(index(), index())
        assertEquals(index().hashCode(), index().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            index(textDirection = null),
            index(books = null),
        ).forEach {
            assertNotEquals(index(), it)
            assertNotEquals(index().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("BibleVersionIndex(textDirection=rtl, books=[])", index().toString())
    }
}
