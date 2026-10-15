package com.youversion.platform.core.verseimages.api

import kotlin.test.Test
import kotlin.test.assertEquals

class VerseImagesEndpointsTests {
    @Test
    fun `test verse images url sends every filter and paging parameter`() {
        assertEquals(
            "https://api.youversion.com/v1/images" +
                "?passage_id=1CO.13.4-7" +
                "&language_ranges%5B%5D=en&language_ranges%5B%5D=es" +
                "&page_size=25&page_token=current-token",
            VerseImagesEndpoints.verseImagesUrl(
                passageId = "1CO.13.4-7",
                languageRanges = listOf("en", "es"),
                pageSize = 25,
                pageToken = "current-token",
            ),
        )
    }

    @Test
    fun `test verse images url omits every parameter that is not supplied`() {
        assertEquals("https://api.youversion.com/v1/images", VerseImagesEndpoints.verseImagesUrl())
    }

    @Test
    fun `test verse image url encodes the opaque id as one path segment`() {
        assertEquals(
            "https://api.youversion.com/v1/images/abc%2Fdef=",
            VerseImagesEndpoints.verseImageUrl(imageId = "abc/def="),
        )
    }
}
