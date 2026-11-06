package com.youversion.platform.core.search.models

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.di.PlatformInternalApi

/**
 * The scripture matching a verse search.
 *
 * @property references The matching references, in rank order.
 * @property userIntent The intent the platform inferred, when it named one.
 * @property didYouMean Alternative spellings offered alongside results for the query as written.
 * @property searchInsteadFor The original wording, present only when the platform corrected the query and
 *     searched for the correction instead.
 * @property nextPageToken A token for the next page, or `null` on the last page.
 */
public class VerseSearchResults
    @PlatformInternalApi
    constructor(
        public val references: List<BibleReference>,
        public val userIntent: SearchUserIntent?,
        public val didYouMean: List<String>,
        public val searchInsteadFor: String?,
        public val nextPageToken: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is VerseSearchResults) return false
            return references == other.references &&
                userIntent == other.userIntent &&
                didYouMean == other.didYouMean &&
                searchInsteadFor == other.searchInsteadFor &&
                nextPageToken == other.nextPageToken
        }

        override fun hashCode(): Int {
            var result = references.hashCode()
            result = 31 * result + userIntent.hashCode()
            result = 31 * result + didYouMean.hashCode()
            result = 31 * result + searchInsteadFor.hashCode()
            result = 31 * result + nextPageToken.hashCode()
            return result
        }

        override fun toString(): String =
            "VerseSearchResults(references=$references, userIntent=$userIntent, didYouMean=$didYouMean, " +
                "searchInsteadFor=$searchInsteadFor, nextPageToken=$nextPageToken)"
    }
