package com.youversion.platform.core.search.api

import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parseApiResponse
import com.youversion.platform.core.search.models.SearchQuery
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.path

internal object SearchEndpoints : SearchApi {
    private val languageRangeRegex = Regex("[A-Za-z]{1,8}(-[0-9A-Za-z]{1,8})*")

    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

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

    override suspend fun suggestedQueries(
        query: String,
        languageRanges: List<String>,
    ): List<SearchQuery> {
        require(query.isNotEmpty()) { "query must not be empty" }
        return queries(languageRanges = languageRanges, query = query, isTrending = false)
    }

    override suspend fun trendingQueries(languageRanges: List<String>): List<SearchQuery> =
        queries(languageRanges = languageRanges, query = null, isTrending = true)

    private suspend fun queries(
        languageRanges: List<String>,
        query: String?,
        isTrending: Boolean,
    ): List<SearchQuery> {
        require(languageRanges.isNotEmpty()) { "languageRanges must not be empty" }
        require(languageRanges.all(::isValidLanguageRange)) {
            "languageRanges must each be a canonical BCP 47 language tag, or \"*\""
        }
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

    private fun isValidLanguageRange(languageRange: String): Boolean =
        languageRange == "*" || languageRangeRegex.matches(languageRange)

    private fun URLBuilder.languageRanges(languageRanges: List<String>) {
        languageRanges.forEach { languageRange -> parameters.append("language_ranges[]", languageRange) }
    }
}
