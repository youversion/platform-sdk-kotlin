package com.youversion.platform.core.search.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ImageSearchResultsTests {
    private val image = SearchImage(id = "img-1", passageId = "JHN.3.16")

    @Test
    fun `test results with equal images and tokens are equal`() {
        val first = ImageSearchResults(images = listOf(image), nextPageToken = "next")
        val second =
            ImageSearchResults(
                images = listOf(SearchImage(id = "img-1", passageId = "JHN.3.16")),
                nextPageToken = "next",
            )
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `test results differing only in images are not equal`() {
        val other = SearchImage(id = "img-2", passageId = "JHN.3.16")
        assertNotEquals(
            ImageSearchResults(images = listOf(image), nextPageToken = "next"),
            ImageSearchResults(images = listOf(other), nextPageToken = "next"),
        )
    }

    @Test
    fun `test results differing only in next page token are not equal`() {
        assertNotEquals(
            ImageSearchResults(images = listOf(image), nextPageToken = "next"),
            ImageSearchResults(images = listOf(image), nextPageToken = null),
        )
    }

    @Test
    fun `test to string includes images and next page token`() {
        val text = ImageSearchResults(images = listOf(image), nextPageToken = "next").toString()
        assertTrue(text.contains("images=[SearchImage(id=img-1, passageId=JHN.3.16)]"))
        assertTrue(text.contains("nextPageToken=next"))
    }
}
