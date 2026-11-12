package com.youversion.platform.core.api

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ApiResponse<T>(
    @SerialName("data") val data: T,
)

@Serializable
public class PaginatedResponse<T>
    @PlatformInternalApi
    constructor(
        @SerialName("data") public val data: List<T>,
        @SerialName("next_page_token") public val nextPageToken: String? = null,
        @SerialName("total_size") public val totalSize: Int? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PaginatedResponse<*>) return false
            return data == other.data &&
                nextPageToken == other.nextPageToken &&
                totalSize == other.totalSize
        }

        override fun hashCode(): Int {
            var result = data.hashCode()
            result = 31 * result + nextPageToken.hashCode()
            result = 31 * result + totalSize.hashCode()
            return result
        }

        override fun toString(): String =
            "PaginatedResponse(data=$data, nextPageToken=$nextPageToken, totalSize=$totalSize)"
    }
