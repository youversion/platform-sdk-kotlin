package com.youversion.platform.core.search.models

/**
 * A search string a reader might run.
 *
 * @property text The query to run or display.
 * @property source Where the query came from, when the platform names it.
 */
data class SearchQuery(
    val text: String,
    val source: String?,
)
