package com.youversion.platform.core.videos.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.deviceLanguageRanges
import com.youversion.platform.core.videos.models.Video
import com.youversion.platform.core.videos.models.VideoPlaylist
import com.youversion.platform.core.videos.models.VideoThumbnailSize

public interface VideosApi {
    /**
     * Fetches one page of an organization's playlists.
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * Keep the same [fields] and [pageSize] when continuing with a [pageToken]. A response with no content is an
     * empty page.
     *
     * @param organizationId The organization whose playlists to fetch. Opaque; it is encoded as given.
     * @param fields The fields to return — any of `id`, `title`, `description`, `type`, `total` and `videos`.
     *     `null` returns every field; fields left out are `null` on each playlist.
     * @param pageSize The number of playlists to return. When supplied, it must be between 1 and 99. `null` uses the
     *     platform default, except that naming one to three [fields] returns every remaining playlist.
     * @param pageToken The [PaginatedResponse.nextPageToken] of the previous page.
     * @param languageRanges BCP 47 language ranges in preference order that choose the language of titles and
     *     descriptions. Sent as `Accept-Language`, with a descending quality value on every range after the first;
     *     only the first ten ranges are sent and blank ranges are dropped. It defaults to the device's language, then
     *     that language without its region, then `en`, such as `es-MX, es, en`. A list with no non-blank range leaves
     *     the header off, which can currently return an empty page for an organization that has playlists.
     * @return The page of [VideoPlaylist]s.
     * @throws IllegalArgumentException if [organizationId] is empty, [fields] is empty or names an unknown field, or
     *     [pageSize] is outside the range stated above.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    public suspend fun playlists(
        organizationId: String,
        fields: List<String>? = null,
        pageSize: Int? = null,
        pageToken: String? = null,
        languageRanges: List<String> = deviceLanguageRanges(),
    ): PaginatedResponse<VideoPlaylist>

    /**
     * Fetches one of an organization's playlists.
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * @param organizationId The organization that owns the playlist. Opaque; it is encoded as given.
     * @param id The playlist's id.
     * @param languageRanges BCP 47 language ranges in preference order that choose the language of the title and
     *     description. Sent as `Accept-Language`, with a descending quality value on every range after the first;
     *     only the first ten ranges are sent and blank ranges are dropped. It defaults to the device's language, then
     *     that language without its region, then `en`. A list with no non-blank range leaves the header off.
     * @return The [VideoPlaylist].
     * @throws IllegalArgumentException if [organizationId] is empty.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    public suspend fun playlist(
        organizationId: String,
        id: Int,
        languageRanges: List<String> = deviceLanguageRanges(),
    ): VideoPlaylist

    /**
     * Fetches a video's metadata, thumbnail and playback streams.
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * @param id The video's id. Opaque; it is encoded as given.
     * @param thumbnailSize The rendition of the thumbnail to link to. `null` removes the size from the thumbnail URL,
     *     so the platform serves its default rendition.
     * @return The [Video].
     * @throws IllegalArgumentException if [id] is empty.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response,
     *     including a thumbnail URL that cannot be parsed.
     */
    public suspend fun video(
        id: String,
        thumbnailSize: VideoThumbnailSize? = null,
    ): Video
}
