package com.youversion.platform.core.search.models

import com.youversion.platform.core.verseimages.api.VerseImagesApi

/**
 * An image matching a search, carrying only enough to identify it.
 *
 * @property id The identifier to pass to [VerseImagesApi.verseImage] to get the image's URL, size and attribution.
 * @property passageId The verse or verse range the image illustrates, such as `JHN.3.16` or `1CO.13.4-7`.
 */
class SearchImage(
    val id: String,
    val passageId: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchImage) return false
        return id == other.id && passageId == other.passageId
    }

    override fun hashCode(): Int = 31 * id.hashCode() + passageId.hashCode()

    override fun toString(): String = "SearchImage(id=$id, passageId=$passageId)"
}
