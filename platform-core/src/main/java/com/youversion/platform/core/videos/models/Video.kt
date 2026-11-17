package com.youversion.platform.core.videos.models

import com.youversion.platform.core.di.PlatformInternalApi
import io.ktor.http.URLBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A video's metadata and the streams the app may play.
 *
 * @property id The id issued with this response. Use the latest one returned for later requests; it is not a
 *     stable identity.
 * @property title The display title.
 * @property description The display description.
 * @property references The USFM references the video covers, including ranges and combined references.
 * @property passageId The chapter, verse or range the video covers, when it covers a single passage.
 * @property languageTag The BCP 47 language tag of the video.
 * @property runtime The running time in seconds.
 * @property orientation The video's orientation, as the platform names it.
 * @property thumbnail The still image to show before playback.
 * @property playbackSources The full-length streams.
 * @property playbackPreviewSources The preview streams.
 * @property shareUrl The public link to share.
 */
@Serializable
public class Video
    @PlatformInternalApi
    constructor(
        @SerialName("id") public val id: String,
        @SerialName("title") public val title: String? = null,
        @SerialName("description") public val description: String? = null,
        @SerialName("references") public val references: List<String>? = null,
        @SerialName("passage_id") public val passageId: String? = null,
        @SerialName("language_tag") public val languageTag: String? = null,
        @SerialName("runtime") public val runtime: Int? = null,
        @SerialName("orientation") public val orientation: String? = null,
        @SerialName("thumbnail") public val thumbnail: VideoThumbnail? = null,
        @SerialName("playback_sources") public val playbackSources: List<VideoPlaybackSource>,
        @SerialName("playback_preview_sources") public val playbackPreviewSources: List<VideoPlaybackSource>,
        @SerialName("share_url") public val shareUrl: String? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Video) return false
            return id == other.id &&
                title == other.title &&
                description == other.description &&
                references == other.references &&
                passageId == other.passageId &&
                languageTag == other.languageTag &&
                runtime == other.runtime &&
                orientation == other.orientation &&
                thumbnail == other.thumbnail &&
                playbackSources == other.playbackSources &&
                playbackPreviewSources == other.playbackPreviewSources &&
                shareUrl == other.shareUrl
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + references.hashCode()
            result = 31 * result + passageId.hashCode()
            result = 31 * result + languageTag.hashCode()
            result = 31 * result + (runtime ?: 0)
            result = 31 * result + orientation.hashCode()
            result = 31 * result + thumbnail.hashCode()
            result = 31 * result + playbackSources.hashCode()
            result = 31 * result + playbackPreviewSources.hashCode()
            result = 31 * result + shareUrl.hashCode()
            return result
        }

        override fun toString(): String =
            "Video(id=$id, title=$title, description=$description, references=$references, passageId=$passageId, " +
                "languageTag=$languageTag, runtime=$runtime, orientation=$orientation, thumbnail=$thumbnail, " +
                "playbackSources=$playbackSources, playbackPreviewSources=$playbackPreviewSources, shareUrl=$shareUrl)"

        /** Replaces every `size` query item on the thumbnail URL with [size], or drops them when [size] is `null`. */
        internal fun withThumbnailSize(size: String?): Video {
            val thumbnail = thumbnail ?: return this
            val url =
                URLBuilder(thumbnail.url ?: return this)
                    .apply {
                        encodedParameters.remove("size")
                        size?.let { encodedParameters.append("size", it) }
                    }.buildString()
            return Video(
                id,
                title,
                description,
                references,
                passageId,
                languageTag,
                runtime,
                orientation,
                VideoThumbnail(url, thumbnail.width, thumbnail.height),
                playbackSources,
                playbackPreviewSources,
                shareUrl,
            )
        }
    }

/**
 * A video's still image and the source image's native size.
 *
 * @property url Where the image is served, at the size the request asked for.
 * @property width The native width in pixels.
 * @property height The native height in pixels.
 */
@Serializable
public class VideoThumbnail
    @PlatformInternalApi
    constructor(
        @SerialName("url") public val url: String? = null,
        @SerialName("width") public val width: Int? = null,
        @SerialName("height") public val height: Int? = null,
    ) {
        /** [width] divided by [height], or `null` unless both are known and positive. */
        public val aspectRatio: Double?
            get() = if (width != null && height != null && width > 0 && height > 0) width.toDouble() / height else null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is VideoThumbnail) return false
            return url == other.url && width == other.width && height == other.height
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + (width ?: 0)
            result = 31 * result + (height ?: 0)
            return result
        }

        override fun toString(): String = "VideoThumbnail(url=$url, width=$width, height=$height)"
    }

/**
 * One stream of a video.
 *
 * @property url Where the stream is served.
 * @property format The stream's format as the platform names it, such as `hls`, `webm`, `mp4` or `mp3`.
 */
@Serializable
public class VideoPlaybackSource
    @PlatformInternalApi
    constructor(
        @SerialName("url") public val url: String,
        @SerialName("format") public val format: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is VideoPlaybackSource) return false
            return url == other.url && format == other.format
        }

        override fun hashCode(): Int = 31 * url.hashCode() + format.hashCode()

        override fun toString(): String = "VideoPlaybackSource(url=$url, format=$format)"
    }
