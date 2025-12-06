package com.youversion.platform.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    @SerialName("data") val data: T,
)

@Serializable
data class PaginatedResponse<T>(
    @SerialName("data") val data: List<T>,
    @SerialName("next_page_token") val nextPageToken: String? = null,
    @SerialName("total_size") val totalSize: Int? = null,
)
