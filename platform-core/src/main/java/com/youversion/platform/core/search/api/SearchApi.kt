package com.youversion.platform.core.search.api

import com.youversion.platform.core.search.models.SearchQuery
import com.youversion.platform.core.search.models.SearchUserIntent
import com.youversion.platform.core.search.models.TopicSearchResults
import com.youversion.platform.core.search.models.VerseSearchResults

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

    /**
     * Retrieves the scripture in the Bible version identified by [bibleId] that matches [query].
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * Unlike [suggestedQueries] and [trendingQueries], a response carrying no content is not treated as an empty
     * result here; it surfaces as a [com.youversion.platform.core.api.YouVersionNetworkException]. That asymmetry is
     * deliberate and matches the other YouVersion Platform SDKs.
     *
     * A result the platform returns that does not name a book, chapter and verse is dropped, so that one malformed
     * entry does not cost the reader the rest of the results.
     *
     * @param query The text to search for. It must be between 1 and 100 grapheme clusters, so that it is accepted or
     *     rejected identically on every platform.
     * @param bibleId The identifier of the Bible version to search. It must be greater than zero.
     * @param userIntent The kind of thing the reader is believed to be looking for, used to rank the results.
     *     Defaults to [SearchUserIntent.unknown].
     * @param pageSize The number of results to return. When supplied, it must be between 1 and 99.
     * @param pageToken A [VerseSearchResults.nextPageToken] from an earlier search, to continue where it left off.
     * @return The matching [VerseSearchResults].
     * @throws IllegalArgumentException if [query], [bibleId] or [pageSize] is outside the range stated above. An
     *     argument error is a mistake in the calling code rather than a network condition, so it is reported
     *     separately from one.
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
     * Unlike [suggestedQueries] and [trendingQueries], a response carrying no content is not treated as an empty
     * result here; it surfaces as a [com.youversion.platform.core.api.YouVersionNetworkException]. That asymmetry is
     * deliberate and matches the other YouVersion Platform SDKs.
     *
     * @param query The text to search for. It must be between 1 and 100 grapheme clusters, so that it is accepted or
     *     rejected identically on every platform.
     * @param languageRanges An ordered list of canonical BCP 47 language tags, such as `en-US`, or `*` to match
     *     all languages. It must not be empty.
     * @return The matching [TopicSearchResults]. Topic results do not page.
     * @throws IllegalArgumentException if [query] is outside the range stated above, or if [languageRanges] is empty
     *     or holds a range that is neither `*` nor a well-formed BCP 47 tag. An argument error is a mistake in the
     *     calling code rather than a network condition, so it is reported separately from one.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun topics(
        query: String,
        languageRanges: List<String>,
    ): TopicSearchResults
}
