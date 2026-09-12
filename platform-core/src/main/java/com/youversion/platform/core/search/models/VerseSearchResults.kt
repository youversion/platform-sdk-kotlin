package com.youversion.platform.core.search.models

import com.youversion.platform.core.bibles.domain.BibleReference

/**
 * The scripture matching a verse search, together with the metadata the platform returned about the query itself.
 *
 * @property references The matching references, in the order the platform ranked them.
 * @property userIntent The intent the platform inferred from the query, when it named one.
 * @property didYouMean Alternative spellings offered alongside results for the query as it was written.
 * @property searchInsteadFor The original wording, present only when the platform corrected the query and returned
 *     results for the correction instead.
 * @property nextPageToken A token for the following page of results, or `null` when this is the last page.
 */
data class VerseSearchResults(
    val references: List<BibleReference>,
    val userIntent: SearchUserIntent?,
    val didYouMean: List<String>,
    val searchInsteadFor: String?,
    val nextPageToken: String?,
)
