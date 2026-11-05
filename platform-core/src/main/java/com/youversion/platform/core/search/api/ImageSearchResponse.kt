package com.youversion.platform.core.search.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** [images] takes no default, so a response missing it fails. Each entry stays raw JSON so a malformed one is dropped. */
@Serializable
internal data class ImageSearchResponse(
    @SerialName("images") val images: List<JsonElement>,
    @SerialName("next_page_token") val nextPageToken: String? = null,
)
