package com.youversion.platform.core.search.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SearchQueryTests {
    private fun query(
        text: String = "text",
        source: String? = "source",
    ) = SearchQuery(
        text = text,
        source = source,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(query(), query())
        assertEquals(query().hashCode(), query().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            query(text = "other"),
            query(source = null),
        ).forEach {
            assertNotEquals(query(), it)
            assertNotEquals(query().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("SearchQuery(text=text, source=source)", query().toString())
    }
}
