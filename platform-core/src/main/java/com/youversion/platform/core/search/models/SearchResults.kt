package com.youversion.platform.core.search.models

import com.youversion.platform.core.bibles.domain.BibleReference

/**
 * The scripture and topics matching a unified search. Unified results do not page.
 *
 * @property references The matching references, in rank order.
 * @property topics The matching topics, in rank order.
 * @property userIntent The intent the platform inferred, when it named one.
 * @property didYouMean Alternative spellings offered alongside results for the query as written.
 * @property searchInsteadFor The original wording, present only when the platform corrected the query and
 *     searched for the correction instead.
 */
data class SearchResults(
    val references: List<BibleReference>,
    val topics: List<SearchTopic>,
    val userIntent: SearchUserIntent?,
    val didYouMean: List<String>,
    val searchInsteadFor: String?,
)
