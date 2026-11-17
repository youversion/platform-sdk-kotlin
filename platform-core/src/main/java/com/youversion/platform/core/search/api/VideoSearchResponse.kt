package com.youversion.platform.core.search.api

import com.youversion.platform.core.videos.models.Video
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Collections take no default: a response missing one must fail decoding, not arrive empty. */
@Serializable
internal data class VideoSearchResponse(
    @SerialName("videos") val videos: List<Video>,
    @SerialName("user_intent") val userIntent: String? = null,
    @SerialName("did_you_mean") val didYouMean: List<String>,
    @SerialName("search_instead_for") val searchInsteadFor: String? = null,
    @SerialName("next_page_token") val nextPageToken: String? = null,
)
