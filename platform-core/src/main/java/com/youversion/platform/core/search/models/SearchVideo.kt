package com.youversion.platform.core.search.models

import com.youversion.platform.core.di.PlatformInternalApi

/**
 * A video matching a search, carrying only enough to identify it.
 *
 * @property id The opaque identifier to pass to [com.youversion.platform.core.videos.api.VideosApi.video] to get the
 *     video's metadata and playback sources.
 */
public class SearchVideo
    @PlatformInternalApi
    constructor(
        public val id: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SearchVideo) return false
            return id == other.id
        }

        override fun hashCode(): Int = id.hashCode()

        override fun toString(): String = "SearchVideo(id=$id)"
    }
