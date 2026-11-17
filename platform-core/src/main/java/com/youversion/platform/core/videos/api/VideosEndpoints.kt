package com.youversion.platform.core.videos.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.fields
import com.youversion.platform.core.api.pageToken
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parseApiBody
import com.youversion.platform.core.api.parsePaginatedResponse
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import com.youversion.platform.core.videos.models.Video
import com.youversion.platform.core.videos.models.VideoPlaylist
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
    private val thumbnailSizes = setOf("thumb", "card", "full")

    /** The playlists URL. Selecting [fields] switches to the beta path; [playlists] checks the arguments. */
    fun playlistsUrl(
        organizationId: String,
        fields: List<String>? = null,
        pageSize: String? = null,
        pageToken: String? = null,
    ): String =
        buildYouVersionUrlString {
            path(if (fields.isNullOrEmpty()) "/v1/organizations" else "/v1-beta/organizations")
            appendPathSegments(listOf(organizationId, "playlists"), encodeSlash = true)
            fields(fields)
            parameter("page_size", pageSize)
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
            path("/v1-beta/videos")
            appendPathSegments(listOf(id), encodeSlash = true)
        }

    /** shortcut: sends `Accept-Language: en` to dodge cached empty 204 responses; remove once the server fix is verified. */
    override suspend fun playlists(
        organizationId: String,
        fields: List<String>?,
        pageSize: String?,
        pageToken: String?,
    ): PaginatedResponse<VideoPlaylist> {
        require(organizationId.isNotEmpty()) { "organizationId must not be empty" }
        fields?.let { require(it.isNotEmpty() && playlistFields.containsAll(it)) { "fields must name known fields" } }
        pageSize?.let {
            require(isValidPlaylistPageSize(it, fields)) {
                "pageSize must be \"1\" to \"99\", or \"*\" with one to three fields"
            }
        }
        return httpClient
            .get(playlistsUrl(organizationId, fields, pageSize, pageToken)) {
                header(HttpHeaders.AcceptLanguage, "en")
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
    ): VideoPlaylist {
        require(organizationId.isNotEmpty()) { "organizationId must not be empty" }
        return parseApiBody(httpClient.get(playlistUrl(organizationId, id)))
    }

    override suspend fun video(
        id: String,
        thumbnailSize: String?,
    ): Video {
        require(id.isNotEmpty()) { "id must not be empty" }
        thumbnailSize?.let { require(it in thumbnailSizes) { "thumbnailSize must be thumb, card or full" } }
        return parseApiBody<Video>(httpClient.get(videoUrl(id))).withThumbnailSize(thumbnailSize)
    }

    private fun isValidPlaylistPageSize(
        pageSize: String,
        fields: List<String>?,
    ): Boolean =
        if (pageSize == "*") {
            fields?.size in 1..3
        } else {
            pageSize.toIntOrNull()?.takeIf { it.toString() == pageSize } in 1..99
        }
}
