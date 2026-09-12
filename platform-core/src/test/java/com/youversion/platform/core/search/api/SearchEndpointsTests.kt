package com.youversion.platform.core.search.api

import com.youversion.platform.core.search.models.SearchUserIntent
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

    @Test
    fun `test verse search url`() {
        assertEquals(
            "https://api.youversion.com/v1/search-verses" +
                "?query=two+fish&bible_id=111&user_intent=text&page_size=25&page_token=current-token",
            SearchEndpoints.searchVersesUrl(
                query = "two fish",
                bibleId = 111,
                userIntent = SearchUserIntent.text,
                pageSize = 25,
                pageToken = "current-token",
            ),
        )
    }

    @Test
    fun `test verse search url omits paging when it is not supplied`() {
        assertEquals(
            "https://api.youversion.com/v1/search-verses?query=love&bible_id=111&user_intent=unknown",
            SearchEndpoints.searchVersesUrl(query = "love", bibleId = 111, userIntent = SearchUserIntent.unknown),
        )
    }

    @Test
    fun `test topic search url repeats every language range`() {
        assertEquals(
            "https://api.youversion.com/v1/search-topics" +
                "?query=faif&language_ranges%5B%5D=en-US&language_ranges%5B%5D=%2A",
            SearchEndpoints.searchTopicsUrl(query = "faif", languageRanges = listOf("en-US", "*")),
        )
    }
}
