package com.youversion.platform.core.search.models

import com.youversion.platform.core.bibles.domain.BibleReference

/**
 * The scripture and the topics matching a unified search, together with the metadata the platform returned about the
 * query itself.
 *
 * Unified results do not page, so there is no next-page token.
 *
 * @property references The matching references, in the order the platform ranked them.
 * @property topics The matching topics, in the order the platform ranked them.
 * @property userIntent The intent the platform inferred from the query, when it named one.
 * @property didYouMean Alternative spellings offered alongside results for the query as it was written.
 * @property searchInsteadFor The original wording, present only when the platform corrected the query and returned
 *     results for the correction instead.
 */
data class SearchResults(
    val references: List<BibleReference>,
    val topics: List<SearchTopic>,
    val userIntent: SearchUserIntent?,
    val didYouMean: List<String>,
    val searchInsteadFor: String?,
)
