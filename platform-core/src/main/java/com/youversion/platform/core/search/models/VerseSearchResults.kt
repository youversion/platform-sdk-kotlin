package com.youversion.platform.core.search.models

import com.youversion.platform.core.bibles.domain.BibleReference

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
data class VerseSearchResults(
    val references: List<BibleReference>,
    val userIntent: SearchUserIntent?,
    val didYouMean: List<String>,
    val searchInsteadFor: String?,
    val nextPageToken: String?,
)
