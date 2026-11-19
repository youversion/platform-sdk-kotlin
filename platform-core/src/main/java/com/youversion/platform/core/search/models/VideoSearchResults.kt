package com.youversion.platform.core.search.models

import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.core.videos.models.Video

/**
 * The videos matching a video search.
 *
 * @property videos The matching videos, in rank order.
 * @property userIntent The intent the platform inferred, when it named one.
 * @property didYouMean Alternative spellings offered alongside results for the query as written.
 * @property searchInsteadFor The original wording, present only when the platform corrected the query and
 *     searched for the correction instead.
 * @property nextPageToken A token for the next page, or `null` on the last page. A page with no videos can still
 *     carry one.
 */
public class VideoSearchResults
    @PlatformInternalApi
    constructor(
        public val videos: List<Video>,
        public val userIntent: SearchUserIntent?,
        public val didYouMean: List<String>,
        public val searchInsteadFor: String?,
        public val nextPageToken: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is VideoSearchResults) return false
            return videos == other.videos &&
                userIntent == other.userIntent &&
                didYouMean == other.didYouMean &&
                searchInsteadFor == other.searchInsteadFor &&
                nextPageToken == other.nextPageToken
        }

        override fun hashCode(): Int {
            var result = videos.hashCode()
            result = 31 * result + userIntent.hashCode()
            result = 31 * result + didYouMean.hashCode()
            result = 31 * result + searchInsteadFor.hashCode()
            result = 31 * result + nextPageToken.hashCode()
            return result
        }

        override fun toString(): String =
            "VideoSearchResults(videos=$videos, userIntent=$userIntent, didYouMean=$didYouMean, " +
                "searchInsteadFor=$searchInsteadFor, nextPageToken=$nextPageToken)"
    }
