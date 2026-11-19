package com.youversion.platform.core.videos.models

/**
 * A rendition of a video's thumbnail. The platform offers only the sizes named in the companion.
 *
 * @property rawValue The size as the platform spells it.
 */
public class VideoThumbnailSize private constructor(
    public val rawValue: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VideoThumbnailSize) return false
        return rawValue == other.rawValue
    }

    override fun hashCode(): Int = rawValue.hashCode()

    override fun toString(): String = "VideoThumbnailSize(rawValue=$rawValue)"

    public companion object {
        /** The smallest rendition, for lists. */
        public val thumb: VideoThumbnailSize = VideoThumbnailSize("thumb")

        /** A medium rendition, for cards. */
        public val card: VideoThumbnailSize = VideoThumbnailSize("card")

        /** The full-size rendition. */
        public val full: VideoThumbnailSize = VideoThumbnailSize("full")
    }
}
