package com.youversion.platform.core.search.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A topic search as the platform returns it. The collection fields deliberately take no default, so that a response
 * missing one fails to decode rather than reaching a reader as an empty result.
 */
@Serializable
internal data class TopicSearchResponse(
    @SerialName("topics") val topics: List<TopicSearchResultResponse>,
    @SerialName("did_you_mean") val didYouMean: List<String>,
    @SerialName("search_instead_for") val searchInsteadFor: String? = null,
)

/** A single match, carrying the topic the platform found and any subtopics that narrow it. */
@Serializable
internal data class TopicSearchResultResponse(
    @SerialName("id") val id: Int? = null,
    @SerialName("text") val text: String,
    @SerialName("subtopics") val subtopics: List<String>,
)
