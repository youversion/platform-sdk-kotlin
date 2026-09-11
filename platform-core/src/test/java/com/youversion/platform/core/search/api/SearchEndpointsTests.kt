package com.youversion.platform.core.search.api

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchEndpointsTests {
    @Test
    fun `test suggested queries url`() {
        assertEquals(
            "https://api.youversion.com/v1/search-queries?language_ranges%5B%5D=en-US&query=love",
            SearchEndpoints.searchQueriesUrl(languageRanges = listOf("en-US"), query = "love"),
        )
    }

    @Test
    fun `test suggested queries url repeats every language range`() {
        assertEquals(
            "https://api.youversion.com/v1/search-queries" +
                "?language_ranges%5B%5D=en-US&language_ranges%5B%5D=es-419&language_ranges%5B%5D=%2A" +
                "&query=love",
            SearchEndpoints.searchQueriesUrl(
                languageRanges = listOf("en-US", "es-419", "*"),
                query = "love",
            ),
        )
    }

    @Test
    fun `test trending queries url sends the trending flag and no query`() {
        assertEquals(
            "https://api.youversion.com/v1/search-queries?language_ranges%5B%5D=%2A&trending=true",
            SearchEndpoints.searchQueriesUrl(languageRanges = listOf("*"), isTrending = true),
        )
    }
}
