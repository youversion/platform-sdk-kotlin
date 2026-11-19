package com.youversion.platform.core.search.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Collections take no default: a response missing one must fail decoding, not arrive empty. [images] is the
 * and [videos] are the exceptions, because the platform omits them wherever those results are not yet offered. They
 * stay raw JSON so a malformed entry, or a malformed list, is dropped rather than failing the whole search.
 */
@Serializable
internal data class UnifiedSearchResponse(
    @SerialName("verses") val references: List<VerseSearchResultResponse>,
    @SerialName("topics") val topics: List<TopicSearchResultResponse>,
    @SerialName("images") val images: JsonElement? = null,
    @SerialName("videos") val videos: JsonElement? = null,
    @SerialName("user_intent") val userIntent: String? = null,
    @SerialName("did_you_mean") val didYouMean: List<String>,
    @SerialName("search_instead_for") val searchInsteadFor: String? = null,
)
