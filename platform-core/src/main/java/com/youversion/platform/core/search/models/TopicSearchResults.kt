package com.youversion.platform.core.search.models

/**
 * The topics matching a topic search, together with the metadata the platform returned about the query itself.
 *
 * Topic results do not page, so there is no next-page token and no total count.
 *
 * @property topics The matching topics, in the order the platform ranked them.
 * @property didYouMean Alternative spellings offered alongside results for the query as it was written.
 * @property searchInsteadFor The original wording, present only when the platform corrected the query and returned
 *     results for the correction instead.
 */
data class TopicSearchResults(
    val topics: List<SearchTopic>,
    val didYouMean: List<String>,
    val searchInsteadFor: String?,
)
