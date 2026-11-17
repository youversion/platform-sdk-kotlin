package com.youversion.platform.core.videos.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.videos.models.Video
import com.youversion.platform.core.videos.models.VideoPlaylist

public interface VideosApi {
    /**
     * Fetches one page of an organization's playlists.
     *
     * Keep the same [fields] and [pageSize] when continuing with a [pageToken]. A response with no content is an
     * empty page.
     *
     * @param organizationId The organization whose playlists to fetch. Opaque; it is encoded as given.
     * @param fields The fields to return — any of `id`, `title`, `description`, `type`, `total` and `videos`.
     *     `null` returns every field; fields left out are `null` on each playlist.
     * @param pageSize `"1"` to `"99"`, or `"*"` for every remaining playlist when one to three [fields] are named.
     *     `null` uses the platform default.
     * @param pageToken The [PaginatedResponse.nextPageToken] of the previous page.
     * @throws IllegalArgumentException if [organizationId] is empty, [fields] is empty or names an unknown field, or
     *     [pageSize] is not allowed.
     */
    public suspend fun playlists(
        organizationId: String,
        fields: List<String>? = null,
        pageSize: String? = null,
        pageToken: String? = null,
    ): PaginatedResponse<VideoPlaylist>

    /**
     * Fetches one of an organization's playlists.
     *
     * @param organizationId The organization that owns the playlist. Opaque; it is encoded as given.
     * @param id The playlist's id.
     * @throws IllegalArgumentException if [organizationId] is empty.
     */
    public suspend fun playlist(
        organizationId: String,
        id: Int,
    ): VideoPlaylist

    /**
     * Fetches a video's metadata, thumbnail and playback streams.
     *
     * @param id The video's id. Opaque; it is encoded as given.
     * @param thumbnailSize The thumbnail size — `thumb`, `card` or `full`. `null` returns the full-size thumbnail.
     * @throws IllegalArgumentException if [id] is empty or [thumbnailSize] is not one of the allowed values.
     */
    public suspend fun video(
        id: String,
        thumbnailSize: String? = null,
    ): Video
}
