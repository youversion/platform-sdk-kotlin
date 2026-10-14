package com.youversion.platform.core.verseimages.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.languageRanges
import com.youversion.platform.core.api.pageSize
import com.youversion.platform.core.api.pageToken
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parseApiBody
import com.youversion.platform.core.api.parsePaginatedResponse
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import com.youversion.platform.core.verseimages.models.VerseImage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import io.ktor.http.path

internal object VerseImagesEndpoints : VerseImagesApi {
    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

    fun verseImagesUrl(
        passageId: String? = null,
        languageRanges: List<String> = emptyList(),
        pageSize: Int? = null,
        pageToken: String? = null,
    ): String =
        buildYouVersionUrlString {
            path("/v1/images")
            parameter("passage_id", passageId)
            languageRanges(languageRanges)
            pageSize(pageSize)
            pageToken(pageToken)
        }

    fun verseImageUrl(imageId: String): String =
        buildYouVersionUrlString {
            path("/v1/images")
            appendPathSegments(listOf(imageId), encodeSlash = true)
        }

    override suspend fun verseImages(
        passageId: String?,
        languageRanges: List<String>,
        pageSize: Int?,
        pageToken: String?,
    ): PaginatedResponse<VerseImage> {
        pageSize?.let { require(it in 1..99) { "pageSize must be between 1 and 99" } }
        return parsePaginatedResponse(
            httpClient.get(verseImagesUrl(passageId, languageRanges, pageSize, pageToken)),
        )
    }

    override suspend fun verseImage(imageId: String): VerseImage {
        require(imageId.isNotEmpty()) { "imageId must not be empty" }
        return parseApiBody(httpClient.get(verseImageUrl(imageId)))
    }
}
