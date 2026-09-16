package com.youversion.platform.core.search.api

import com.youversion.platform.core.search.models.SearchQuery
import com.youversion.platform.core.search.models.SearchResults
import com.youversion.platform.core.search.models.SearchUserIntent
import com.youversion.platform.core.search.models.TopicSearchResults
import com.youversion.platform.core.search.models.VerseSearchResults

interface SearchApi {
    /**
     * Retrieves as-you-type query suggestions matching [query] in the first supported language range.
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * @param query The text the reader has typed so far. It must not be empty, and has no upper length bound.
     * @param languageRanges Canonical BCP 47 tags such as `en-US`, or `*` for any language, in preference order.
     *     It must not be empty.
     * @return The suggested [SearchQuery]s, empty if the platform has none to offer.
     * @throws IllegalArgumentException if [query] is empty, or [languageRanges] is empty or holds a range that is
     *     neither `*` nor a well-formed BCP 47 tag.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun suggestedQueries(
        query: String,
        languageRanges: List<String>,
    ): List<SearchQuery>

    /**
     * Retrieves recently popular queries in the first supported language range.
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * @param languageRanges Canonical BCP 47 tags such as `en-US`, or `*` for any language, in preference order.
     *     It must not be empty.
     * @return The trending [SearchQuery]s, empty if the platform has none to offer.
     * @throws IllegalArgumentException if [languageRanges] is empty or holds a range that is neither `*` nor a
     *     well-formed BCP 47 tag.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun trendingQueries(languageRanges: List<String>): List<SearchQuery>

    /**
     * Retrieves the scripture in the Bible version identified by [bibleId] that matches [query].
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * A response carrying no content fails rather than returning no results, and a result that does not name a book,
     * chapter and verse is dropped.
     *
     * @param query The text to search for. It must be between 1 and 100 grapheme clusters.
     * @param bibleId The identifier of the Bible version to search. It must be greater than zero.
     * @param userIntent The kind of thing the reader is believed to be looking for, used to rank the results.
     * @param pageSize The number of results to return. When supplied, it must be between 1 and 99.
     * @param pageToken A [VerseSearchResults.nextPageToken] from an earlier search, to continue where it left off.
     * @return The matching [VerseSearchResults].
     * @throws IllegalArgumentException if [query], [bibleId] or [pageSize] is outside the range stated above.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun verses(
        query: String,
        bibleId: Int,
        userIntent: SearchUserIntent = SearchUserIntent.unknown,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): VerseSearchResults

    /**
     * Retrieves the topics matching [query] in the first supported language range.
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * A response carrying no content fails rather than returning no results.
     *
     * @param query The text to search for. It must be between 1 and 100 grapheme clusters.
     * @param languageRanges Canonical BCP 47 tags such as `en-US`, or `*` for any language, in preference order.
     *     It must not be empty.
     * @return The matching [TopicSearchResults]. Topic results do not page.
     * @throws IllegalArgumentException if [query] is outside the range stated above, or [languageRanges] is empty
     *     or holds a range that is neither `*` nor a well-formed BCP 47 tag.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun topics(
        query: String,
        languageRanges: List<String>,
    ): TopicSearchResults

    /**
     * Retrieves both the scripture in the Bible version identified by [bibleId] and the topics matching [query], in a
     * single round trip.
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * A response carrying no content fails rather than returning no results, and a result that does not name a book,
     * chapter and verse is dropped.
     *
     * @param query The text to search for. It must be between 1 and 100 grapheme clusters.
     * @param bibleId The identifier of the Bible version to search. It must be greater than zero.
     * @param languageRanges Canonical BCP 47 tags such as `en-US`, or `*` for any language, in preference order.
     *     It must not be empty.
     * @param userIntent The kind of thing the reader is believed to be looking for, used to rank the results.
     * @param fields The kinds of result to return — `verses`, `topics`, or both. Empty asks for every kind. Name
     *     only the kinds you will display, so you are not paying for results you discard.
     * @return The matching [SearchResults]. Unified results do not page.
     * @throws IllegalArgumentException if [query] or [bibleId] is outside the range stated above, or
     *     [languageRanges] is empty or holds a range that is neither `*` nor a well-formed BCP 47 tag.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun unified(
        query: String,
        bibleId: Int,
        languageRanges: List<String>,
        userIntent: SearchUserIntent = SearchUserIntent.unknown,
        fields: List<String> = emptyList(),
    ): SearchResults
}
