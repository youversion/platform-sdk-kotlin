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
 */
data class SearchResults(
    val references: List<BibleReference>,
    val topics: List<SearchTopic>,
    val userIntent: SearchUserIntent?,
    val didYouMean: List<String>,
    val searchInsteadFor: String?,
) {
    /**
     * The matching images, in rank order. Empty when the platform does not offer image results.
     * Not carried over by [copy].
     */
    var images: List<SearchImage> = emptyList()
        private set

    /** Creates results with [images], the only way to set them; the five-argument constructor leaves them empty. */
    constructor(
        references: List<BibleReference>,
        topics: List<SearchTopic>,
        userIntent: SearchUserIntent?,
        didYouMean: List<String>,
        searchInsteadFor: String?,
        images: List<SearchImage>,
    ) : this(references, topics, userIntent, didYouMean, searchInsteadFor) {
        this.images = images
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchResults) return false
        return references == other.references &&
            topics == other.topics &&
            userIntent == other.userIntent &&
            didYouMean == other.didYouMean &&
            searchInsteadFor == other.searchInsteadFor &&
            images == other.images
    }

    override fun hashCode(): Int {
        var result = references.hashCode()
        result = 31 * result + topics.hashCode()
        result = 31 * result + (userIntent?.hashCode() ?: 0)
        result = 31 * result + didYouMean.hashCode()
        result = 31 * result + (searchInsteadFor?.hashCode() ?: 0)
        result = 31 * result + images.hashCode()
        return result
    }

    override fun toString(): String =
        "SearchResults(references=$references, topics=$topics, userIntent=$userIntent, " +
            "didYouMean=$didYouMean, searchInsteadFor=$searchInsteadFor, images=$images)"
}
