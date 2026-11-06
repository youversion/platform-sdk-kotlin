package com.youversion.platform.core.search.models

import com.youversion.platform.core.di.PlatformInternalApi

/**
 * The images matching an image search.
 *
 * @property images The matching images, in rank order.
 * @property nextPageToken A token for the next page, or `null` on the last page. Pass it back with the same query,
 *     Bible version and page size.
 */
public class ImageSearchResults
    @PlatformInternalApi
    constructor(
        public val images: List<SearchImage>,
        public val nextPageToken: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ImageSearchResults) return false
            return images == other.images && nextPageToken == other.nextPageToken
        }

        override fun hashCode(): Int = 31 * images.hashCode() + (nextPageToken?.hashCode() ?: 0)

        override fun toString(): String = "ImageSearchResults(images=$images, nextPageToken=$nextPageToken)"
    }
