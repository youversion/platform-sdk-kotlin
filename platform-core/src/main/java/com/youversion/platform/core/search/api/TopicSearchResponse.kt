package com.youversion.platform.core.search.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Collections take no default: a response missing one must fail decoding, not arrive empty. */
@Serializable
internal data class TopicSearchResponse(
    @SerialName("topics") val topics: List<TopicSearchResultResponse>,
    @SerialName("did_you_mean") val didYouMean: List<String>,
    @SerialName("search_instead_for") val searchInsteadFor: String? = null,
)

@Serializable
internal data class TopicSearchResultResponse(
    @SerialName("id") val id: Int? = null,
    @SerialName("text") val text: String,
    @SerialName("subtopics") val subtopics: List<String>,
)
