package com.youversion.platform.core.search.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Collections take no default: a response missing one must fail decoding, not arrive empty. [images] is the
 * exception, because the platform omits it wherever image results are not yet offered.
 */
@Serializable
internal data class UnifiedSearchResponse(
    @SerialName("verses") val references: List<VerseSearchResultResponse>,
    @SerialName("topics") val topics: List<TopicSearchResultResponse>,
    @SerialName("images") val images: List<ImageSearchResultResponse> = emptyList(),
    @SerialName("user_intent") val userIntent: String? = null,
    @SerialName("did_you_mean") val didYouMean: List<String>,
    @SerialName("search_instead_for") val searchInsteadFor: String? = null,
)

/** Fields are nullable so a malformed image is dropped rather than failing the whole search. */
@Serializable
internal data class ImageSearchResultResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("passage_id") val passageId: String? = null,
)
