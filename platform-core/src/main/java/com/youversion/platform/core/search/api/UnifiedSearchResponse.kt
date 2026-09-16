package com.youversion.platform.core.search.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Collections take no default: a response missing one must fail decoding, not arrive empty. */
@Serializable
internal data class UnifiedSearchResponse(
    @SerialName("verses") val references: List<VerseSearchResultResponse>,
    @SerialName("topics") val topics: List<TopicSearchResultResponse>,
    @SerialName("user_intent") val userIntent: String? = null,
    @SerialName("did_you_mean") val didYouMean: List<String>,
    @SerialName("search_instead_for") val searchInsteadFor: String? = null,
)
