package com.youversion.platform.core.search.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TopicSearchResultsTests {
    private fun results(
        topics: List<SearchTopic> = emptyList(),
        didYouMean: List<String> = listOf("a"),
        searchInsteadFor: String? = "instead",
    ) = TopicSearchResults(
        topics = topics,
        didYouMean = didYouMean,
        searchInsteadFor = searchInsteadFor,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(results(), results())
        assertEquals(results().hashCode(), results().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            results(topics = listOf(SearchTopic(1, "text", emptyList()))),
            results(didYouMean = emptyList()),
            results(searchInsteadFor = null),
        ).forEach {
            assertNotEquals(results(), it)
            assertNotEquals(results().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("TopicSearchResults(topics=[], didYouMean=[a], searchInsteadFor=instead)", results().toString())
    }
}
