package com.youversion.platform.core.search.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A unified search as the platform returns it. The collection fields deliberately take no default, so that a response
 * missing one fails to decode rather than reaching a reader as an empty result.
 */
@Serializable
internal data class UnifiedSearchResponse(
    @SerialName("verses") val references: List<VerseSearchResultResponse>,
    @SerialName("topics") val topics: List<TopicSearchResultResponse>,
    @SerialName("user_intent") val userIntent: String? = null,
    @SerialName("did_you_mean") val didYouMean: List<String>,
    @SerialName("search_instead_for") val searchInsteadFor: String? = null,
)
