package com.youversion.platform.core.search.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SearchResultsTests {
    private fun results(images: List<SearchImage>) =
        SearchResults(
            references = emptyList(),
            topics = emptyList(),
            userIntent = null,
            didYouMean = emptyList(),
            searchInsteadFor = null,
            images = images,
        )

    @Test
    fun `test results with equal images are equal`() {
        val first = results(listOf(SearchImage(id = "img-1", passageId = "JHN.3.16")))
        val second = results(listOf(SearchImage(id = "img-1", passageId = "JHN.3.16")))
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `test results differing only in images are not equal`() {
        val first = results(listOf(SearchImage(id = "img-1", passageId = "JHN.3.16")))
        val second = results(listOf(SearchImage(id = "img-2", passageId = "JHN.3.16")))
        assertNotEquals(first, second)
    }

    @Test
    fun `test to string includes images`() {
        val text = results(listOf(SearchImage(id = "img-1", passageId = "JHN.3.16"))).toString()
        assertTrue(text.contains("images=[SearchImage(id=img-1, passageId=JHN.3.16)]"))
    }

    @Test
    fun `test copy drops images`() {
        val copied = results(listOf(SearchImage(id = "img-1", passageId = "JHN.3.16"))).copy()
        assertEquals(emptyList(), copied.images)
    }

    @Test
    fun `test five argument constructor leaves images empty`() {
        val results =
            SearchResults(
                references = emptyList(),
                topics = emptyList(),
                userIntent = null,
                didYouMean = emptyList(),
                searchInsteadFor = null,
            )
        assertEquals(emptyList(), results.images)
    }
}
