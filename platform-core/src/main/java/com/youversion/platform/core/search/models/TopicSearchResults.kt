package com.youversion.platform.core.search.models

/**
 * The topics matching a topic search. Topic results do not page.
 *
 * @property topics The matching topics, in rank order.
 * @property didYouMean Alternative spellings offered alongside results for the query as written.
 * @property searchInsteadFor The original wording, present only when the platform corrected the query and
 *     searched for the correction instead.
 */
data class TopicSearchResults(
    val topics: List<SearchTopic>,
    val didYouMean: List<String>,
    val searchInsteadFor: String?,
)
