package com.youversion.platform.reader.domain

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.search.models.SearchQuery
import com.youversion.platform.core.search.models.VerseSearchResults
import kotlinx.serialization.json.Json

internal class SearchRepository(
    private val storage: Storage,
) {
    companion object {
        private const val KEY_RECENT_SEARCHES = "bible-reader-search--recent-queries"
        private const val MAX_RECENT_SEARCHES = 3
    }

    // ----- Remote

    suspend fun trendingQueries(languageRange: String): List<SearchQuery> =
        YouVersionApi.search.trendingQueries(languageRanges = listOf(languageRange))

    suspend fun suggestedQueries(
        query: String,
        languageRange: String,
    ): List<SearchQuery> = YouVersionApi.search.suggestedQueries(query = query, languageRanges = listOf(languageRange))

    suspend fun verses(
        query: String,
        bibleId: Int,
        pageToken: String? = null,
    ): VerseSearchResults = YouVersionApi.search.verses(query = query, bibleId = bibleId, pageToken = pageToken)

    // ----- Local

    /** Newest first. */
    val recentSearches: List<String>
        get() =
            storage
                .getStringOrNull(KEY_RECENT_SEARCHES)
                ?.let { runCatching { Json.decodeFromString<List<String>>(it) }.getOrNull() }
                .orEmpty()

    fun recordRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val updated =
            (listOf(trimmed) + recentSearches.filterNot { it.equals(trimmed, ignoreCase = true) })
                .take(MAX_RECENT_SEARCHES)

        storage.putString(KEY_RECENT_SEARCHES, Json.encodeToString(updated))
    }
}
