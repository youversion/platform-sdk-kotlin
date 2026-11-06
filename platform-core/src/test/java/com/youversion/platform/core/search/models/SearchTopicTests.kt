package com.youversion.platform.core.search.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SearchTopicTests {
    private fun topic(
        id: Int? = 1,
        text: String = "text",
        subtopics: List<String> = listOf("a"),
    ) = SearchTopic(
        id = id,
        text = text,
        subtopics = subtopics,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(topic(), topic())
        assertEquals(topic().hashCode(), topic().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            topic(id = null),
            topic(text = "other"),
            topic(subtopics = emptyList()),
        ).forEach {
            assertNotEquals(topic(), it)
            assertNotEquals(topic().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("SearchTopic(id=1, text=text, subtopics=[a])", topic().toString())
    }
}
