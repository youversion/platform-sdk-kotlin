package com.youversion.platform.core.search.models

import com.youversion.platform.core.bibles.domain.BibleReference

/**
 * The scripture, topics and images matching a unified search. Unified results do not page.
 *
 * @property references The matching references, in rank order.
 * @property topics The matching topics, in rank order.
 * @property userIntent The intent the platform inferred, when it named one.
 * @property didYouMean Alternative spellings offered alongside results for the query as written.
 * @property searchInsteadFor The original wording, present only when the platform corrected the query and
 *     searched for the correction instead.
 * @property images The matching images, in rank order.
 */
data class SearchResults
    @JvmOverloads
    constructor(
        val references: List<BibleReference>,
        val topics: List<SearchTopic>,
        val userIntent: SearchUserIntent?,
        val didYouMean: List<String>,
        val searchInsteadFor: String?,
        val images: List<SearchImage> = emptyList(),
    ) {
        @Deprecated("Binary compatibility with 2.6.0; removed in 3.0.0", level = DeprecationLevel.HIDDEN)
        fun copy(
            references: List<BibleReference> = this.references,
            topics: List<SearchTopic> = this.topics,
            userIntent: SearchUserIntent? = this.userIntent,
            didYouMean: List<String> = this.didYouMean,
            searchInsteadFor: String? = this.searchInsteadFor,
        ): SearchResults = copy(references, topics, userIntent, didYouMean, searchInsteadFor, images)
    }
