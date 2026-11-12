package com.youversion.platform.core.search.models

import com.youversion.platform.core.di.PlatformInternalApi

/**
 * The topics matching a topic search. Topic results do not page.
 *
 * @property topics The matching topics, in rank order.
 * @property didYouMean Alternative spellings offered alongside results for the query as written.
 * @property searchInsteadFor The original wording, present only when the platform corrected the query and
 *     searched for the correction instead.
 */
public class TopicSearchResults
    @PlatformInternalApi
    constructor(
        public val topics: List<SearchTopic>,
        public val didYouMean: List<String>,
        public val searchInsteadFor: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TopicSearchResults) return false
            return topics == other.topics &&
                didYouMean == other.didYouMean &&
                searchInsteadFor == other.searchInsteadFor
        }

        override fun hashCode(): Int {
            var result = topics.hashCode()
            result = 31 * result + didYouMean.hashCode()
            result = 31 * result + searchInsteadFor.hashCode()
            return result
        }

        override fun toString(): String =
            "TopicSearchResults(topics=$topics, didYouMean=$didYouMean, searchInsteadFor=$searchInsteadFor)"
    }
