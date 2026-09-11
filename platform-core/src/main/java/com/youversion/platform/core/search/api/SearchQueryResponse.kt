package com.youversion.platform.core.search.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SearchQueryResponse(
    @SerialName("text") val text: String,
    @SerialName("source") val source: String? = null,
)
