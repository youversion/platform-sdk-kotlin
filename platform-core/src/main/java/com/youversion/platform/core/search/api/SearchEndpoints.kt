package com.youversion.platform.core.search.api

import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.fields
import com.youversion.platform.core.api.pageSize
import com.youversion.platform.core.api.pageToken
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parseApiBody
import com.youversion.platform.core.api.parseApiResponse
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.search.models.SearchQuery
import com.youversion.platform.core.search.models.SearchResults
import com.youversion.platform.core.search.models.SearchTopic
import com.youversion.platform.core.search.models.SearchUserIntent
import com.youversion.platform.core.search.models.TopicSearchResults
import com.youversion.platform.core.search.models.VerseSearchResults
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.path
import java.text.BreakIterator

internal object SearchEndpoints : SearchApi {
    private val languageRangeRegex = Regex("[A-Za-z]{1,8}(-[0-9A-Za-z]{1,8})*")

    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

    /** Counts what a reader perceives as characters, not UTF-16 code units; `length` is not equivalent. */
    private val String.graphemeClusterCount: Int
        get() {
            val iterator = BreakIterator.getCharacterInstance()
            iterator.setText(this)
            var count = 0
            while (iterator.next() != BreakIterator.DONE) count++
            return count
        }

    fun searchQueriesUrl(
        languageRanges: List<String>,
        query: String? = null,
        isTrending: Boolean = false,
    ): String =
        buildYouVersionUrlString {
            path("/v1/search-queries")
            languageRanges(languageRanges)
            if (!isTrending) parameter("query", query)
            if (isTrending) parameter("trending", "true")
        }

    fun searchVersesUrl(
        query: String,
        bibleId: Int,
        userIntent: SearchUserIntent,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): String =
        buildYouVersionUrlString {
            path("/v1/search-verses")
            parameter("query", query)
            parameter("bible_id", bibleId)
            parameter("user_intent", userIntent.rawValue)
            pageSize(pageSize)
            pageToken(pageToken)
        }

    fun searchTopicsUrl(
        query: String,
        languageRanges: List<String>,
    ): String =
        buildYouVersionUrlString {
            path("/v1/search-topics")
            parameter("query", query)
            languageRanges(languageRanges)
        }

    fun searchUnifiedUrl(
        query: String,
        bibleId: Int,
        languageRanges: List<String>,
        userIntent: SearchUserIntent,
        fields: List<String>,
    ): String =
        buildYouVersionUrlString {
            path("/v1/search-unified")
            parameter("query", query)
            parameter("bible_id", bibleId)
            languageRanges(languageRanges)
            parameter("user_intent", userIntent.rawValue)
            fields(fields)
        }

    override suspend fun suggestedQueries(
        query: String,
        languageRanges: List<String>,
    ): List<SearchQuery> {
        require(query.isNotEmpty()) { "query must not be empty" }
        return queries(languageRanges = languageRanges, query = query, isTrending = false)
    }

    override suspend fun trendingQueries(languageRanges: List<String>): List<SearchQuery> =
        queries(languageRanges = languageRanges, query = null, isTrending = true)

    override suspend fun verses(
        query: String,
        bibleId: Int,
        userIntent: SearchUserIntent,
        pageSize: Int?,
        pageToken: String?,
    ): VerseSearchResults {
        requireValidQueryLength(query)
        require(bibleId > 0) { "bibleId must be greater than zero" }
        pageSize?.let { require(it in 1..99) { "pageSize must be between 1 and 99" } }

        val response =
            parseApiBody<VerseSearchResponse>(
                httpClient.get(searchVersesUrl(query, bibleId, userIntent, pageSize, pageToken)),
            )
        return VerseSearchResults(
            references = response.references.mapNotNull { it.bibleReference(bibleId) },
            userIntent = response.userIntent?.let(::SearchUserIntent),
            didYouMean = response.didYouMean,
            searchInsteadFor = response.searchInsteadFor,
            nextPageToken = response.nextPageToken,
        )
    }

    override suspend fun topics(
        query: String,
        languageRanges: List<String>,
    ): TopicSearchResults {
        requireValidQueryLength(query)
        requireValidLanguageRanges(languageRanges)

        val response =
            parseApiBody<TopicSearchResponse>(httpClient.get(searchTopicsUrl(query, languageRanges)))
        return TopicSearchResults(
            topics = response.topics.map { it.searchTopic() },
            didYouMean = response.didYouMean,
            searchInsteadFor = response.searchInsteadFor,
        )
    }

    override suspend fun unified(
        query: String,
        bibleId: Int,
        languageRanges: List<String>,
        userIntent: SearchUserIntent,
        fields: List<String>,
    ): SearchResults {
        requireValidQueryLength(query)
        require(bibleId > 0) { "bibleId must be greater than zero" }
        requireValidLanguageRanges(languageRanges)

        val response =
            parseApiBody<UnifiedSearchResponse>(
                httpClient.get(searchUnifiedUrl(query, bibleId, languageRanges, userIntent, fields)),
            )
        return SearchResults(
            references = response.references.mapNotNull { it.bibleReference(bibleId) },
            topics = response.topics.map { it.searchTopic() },
            userIntent = response.userIntent?.let(::SearchUserIntent),
            didYouMean = response.didYouMean,
            searchInsteadFor = response.searchInsteadFor,
        )
    }

    private suspend fun queries(
        languageRanges: List<String>,
        query: String?,
        isTrending: Boolean,
    ): List<SearchQuery> {
        requireValidLanguageRanges(languageRanges)
        return httpClient
            .get(searchQueriesUrl(languageRanges, query, isTrending))
            .let {
                when (it.status) {
                    HttpStatusCode.NoContent -> emptyList()
                    else ->
                        parseApiResponse<List<SearchQueryResponse>>(it).map { response ->
                            SearchQuery(text = response.text, source = response.source)
                        }
                }
            }
    }

    /** The dotted address as a reference in [versionId], or `null` when it is not one. */
    private fun VerseSearchResultResponse.bibleReference(versionId: Int): BibleReference? {
        val components = reference.split(".")
        if (components.size != 3 || components[0].isEmpty()) return null
        val chapter = components[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val verse = components[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
        return BibleReference(
            versionId = versionId,
            bookUSFM = components[0].uppercase(),
            chapter = chapter,
            verse = verse,
        )
    }

    private fun TopicSearchResultResponse.searchTopic(): SearchTopic =
        SearchTopic(id = id, text = text, subtopics = subtopics)

    private fun requireValidQueryLength(query: String) {
        require(query.graphemeClusterCount in 1..100) { "query must be between 1 and 100 grapheme clusters" }
    }

    private fun requireValidLanguageRanges(languageRanges: List<String>) {
        require(languageRanges.isNotEmpty()) { "languageRanges must not be empty" }
        require(languageRanges.all(::isValidLanguageRange)) {
            "languageRanges must each be a canonical BCP 47 language tag, or \"*\""
        }
    }

    private fun isValidLanguageRange(languageRange: String): Boolean =
        languageRange == "*" || languageRangeRegex.matches(languageRange)

    private fun URLBuilder.languageRanges(languageRanges: List<String>) {
        languageRanges.forEach { languageRange -> parameters.append("language_ranges[]", languageRange) }
    }
}
