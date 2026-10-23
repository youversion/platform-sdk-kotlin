package com.youversion.platform.core.verseimages.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.languageRanges
import com.youversion.platform.core.api.pageSize
import com.youversion.platform.core.api.pageToken
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parseApiBody
import com.youversion.platform.core.api.parsePaginatedResponse
import com.youversion.platform.core.api.requireValidPageSize
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import com.youversion.platform.core.verseimages.models.VerseImage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import io.ktor.http.path

internal object VerseImagesEndpoints : VerseImagesApi {
    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

    /**
     * The list URL. Each argument left `null` is omitted, as is an empty [languageRanges]. Arguments are not checked
     * here; [verseImages] checks them.
     */
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

    /**
     * The single-image URL, with [imageId] encoded as one path segment. It is not checked here; [verseImage] checks
     * it.
     */
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
        passageId?.let { require(it.isNotBlank()) { "passageId must not be blank" } }
        requireValidPageSize(pageSize)
        pageToken?.let { require(it.isNotBlank()) { "pageToken must not be blank" } }
        return parsePaginatedResponse(
            httpClient.get(verseImagesUrl(passageId, languageRanges, pageSize, pageToken)),
        )
    }

    override suspend fun verseImage(imageId: String): VerseImage {
        require(imageId.isNotBlank()) { "imageId must not be blank" }
        return parseApiBody(httpClient.get(verseImageUrl(imageId)))
    }
}
