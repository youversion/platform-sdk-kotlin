package com.youversion.platform.core.search.models

/**
 * A search string a user might run and the source that supplied it.
 *
 * @property text The query text to run or display.
 * @property source Where the query came from, when the platform identifies one.
 */
data class SearchQuery(
    val text: String,
    val source: String?,
)
