package com.youversion.platform.core.videos.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.acceptLanguage
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.fields
import com.youversion.platform.core.api.pageToken
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parseApiBody
import com.youversion.platform.core.api.parsePaginatedResponse
import com.youversion.platform.core.api.requireValidPageSize
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import com.youversion.platform.core.videos.models.Video
import com.youversion.platform.core.videos.models.VideoPlaylist
import com.youversion.platform.core.videos.models.VideoThumbnailSize
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.path

internal object VideosEndpoints : VideosApi {
    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

    private val playlistFields = setOf("id", "title", "description", "type", "total", "videos")

    /**
     * The playlists URL. With one to three [fields] and no [pageSize], it asks for every remaining playlist;
     * [playlists] checks the arguments.
     */
    fun playlistsUrl(
        organizationId: String,
        fields: List<String>? = null,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): String =
        buildYouVersionUrlString {
            path("/v1/organizations")
            appendPathSegments(listOf(organizationId, "playlists"), encodeSlash = true)
            fields(fields)
            parameter("page_size", pageSize ?: "*".takeIf { fields?.size in 1..3 })
            pageToken(pageToken)
        }

    /** The URL of one playlist; [organizationId] is encoded as a single path segment. */
    fun playlistUrl(
        organizationId: String,
        id: Int,
    ): String =
        buildYouVersionUrlString {
            path("/v1/organizations")
            appendPathSegments(listOf(organizationId, "playlists", id.toString()), encodeSlash = true)
        }

    /** The URL of one video; [id] is encoded as a single path segment. */
    fun videoUrl(id: String): String =
        buildYouVersionUrlString {
            path("/v1/videos")
            appendPathSegments(listOf(id), encodeSlash = true)
        }

    /**
     * The language defaults to the device's, with English as a fallback, because requests without an
     * `Accept-Language` header can receive a cached empty response even when the organization has playlists. Revisit
     * the default once the server fix for [YPE-6461](https://lifechurch.atlassian.net/browse/YPE-6461) is verified.
     */
    override suspend fun playlists(
        organizationId: String,
        fields: List<String>?,
        pageSize: Int?,
        pageToken: String?,
        languageRanges: List<String>,
    ): PaginatedResponse<VideoPlaylist> {
        require(organizationId.isNotEmpty()) { "organizationId must not be empty" }
        fields?.let { require(it.isNotEmpty() && playlistFields.containsAll(it)) { "fields must name known fields" } }
        requireValidPageSize(pageSize)
        return httpClient
            .get(playlistsUrl(organizationId, fields, pageSize, pageToken)) {
                acceptLanguage(languageRanges)?.let { header(HttpHeaders.AcceptLanguage, it) }
            }.let {
                when (it.status) {
                    HttpStatusCode.NoContent -> PaginatedResponse(emptyList())
                    else -> parsePaginatedResponse(it)
                }
            }
    }

    override suspend fun playlist(
        organizationId: String,
        id: Int,
        languageRanges: List<String>,
    ): VideoPlaylist {
        require(organizationId.isNotEmpty()) { "organizationId must not be empty" }
        return parseApiBody(
            httpClient.get(playlistUrl(organizationId, id)) {
                acceptLanguage(languageRanges)?.let { header(HttpHeaders.AcceptLanguage, it) }
            },
        )
    }

    override suspend fun video(
        id: String,
        thumbnailSize: VideoThumbnailSize?,
    ): Video {
        require(id.isNotEmpty()) { "id must not be empty" }
        return parseApiBody<Video>(httpClient.get(videoUrl(id))).withThumbnailSize(thumbnailSize?.rawValue)
    }
}
