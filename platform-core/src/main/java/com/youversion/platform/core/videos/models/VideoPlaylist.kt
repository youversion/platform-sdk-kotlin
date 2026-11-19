package com.youversion.platform.core.videos.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An organization's ordered playlist. A field left out of the request's field selection is `null`.
 *
 * @property id The playlist's identifier within its organization.
 * @property title The display title.
 * @property description The display description.
 * @property type The kind of playlist, as the platform names it.
 * @property total How many videos the playlist holds.
 * @property videoIds The playlist's videos in order. Pass one to
 *     [VideosApi.video][com.youversion.platform.core.videos.api.VideosApi.video] for its details.
 */
@Serializable
public class VideoPlaylist
    @PlatformInternalApi
    constructor(
        @SerialName("id") public val id: Int? = null,
        @SerialName("title") public val title: String? = null,
        @SerialName("description") public val description: String? = null,
        @SerialName("type") public val type: String? = null,
        @SerialName("total") public val total: Int? = null,
        @SerialName("videos") public val videoIds: List<String>? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is VideoPlaylist) return false
            return id == other.id &&
                title == other.title &&
                description == other.description &&
                type == other.type &&
                total == other.total &&
                videoIds == other.videoIds
        }

        override fun hashCode(): Int {
            var result = id ?: 0
            result = 31 * result + title.hashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + (total ?: 0)
            result = 31 * result + videoIds.hashCode()
            return result
        }

        override fun toString(): String =
            "VideoPlaylist(id=$id, title=$title, description=$description, type=$type, total=$total, " +
                "videoIds=$videoIds)"
    }
