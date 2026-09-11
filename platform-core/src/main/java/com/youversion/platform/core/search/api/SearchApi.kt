package com.youversion.platform.core.search.api

import com.youversion.platform.core.search.models.SearchQuery

interface SearchApi {
    /**
     * Retrieves as-you-type search query suggestions matching [query] in the first supported language range.
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * @param query The text the reader has typed so far. It must not be empty, but it has no upper length
     *     bound, because the reader is still typing it.
     * @param languageRanges An ordered list of canonical BCP 47 language tags, such as `en-US`, or `*` to match
     *     all languages. It must not be empty.
     * @return The suggested [SearchQuery]s, or an empty list if the platform has none to offer.
     * @throws IllegalArgumentException if [query] is empty, or if [languageRanges] is empty or holds a range
     *     that is neither `*` nor a well-formed BCP 47 tag.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun suggestedQueries(
        query: String,
        languageRanges: List<String>,
    ): List<SearchQuery>

    /**
     * Retrieves recently popular search queries in the first supported language range.
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * @param languageRanges An ordered list of canonical BCP 47 language tags, such as `en-US`, or `*` to match
     *     all languages. It must not be empty.
     * @return The trending [SearchQuery]s, or an empty list if the platform has none to offer.
     * @throws IllegalArgumentException if [languageRanges] is empty or holds a range that is neither `*` nor a
     *     well-formed BCP 47 tag.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun trendingQueries(languageRanges: List<String>): List<SearchQuery>
}
