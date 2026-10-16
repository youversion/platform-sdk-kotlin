package com.youversion.platform.core.search.models

/**
 * An image matching a search, carrying only enough to identify it.
 *
 * @property id The opaque identifier of the image.
 * @property passageId The verse or verse range the image illustrates, such as `JHN.3.16` or `1CO.13.4-7`.
 */
class SearchImage(
    val id: String,
    val passageId: String,
)
