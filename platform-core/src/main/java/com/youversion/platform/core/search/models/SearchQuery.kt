package com.youversion.platform.core.search.models

import com.youversion.platform.core.di.PlatformInternalApi

/**
 * A search string a reader might run.
 *
 * @property text The query to run or display.
 * @property source Where the query came from, when the platform names it.
 */
public class SearchQuery
    @PlatformInternalApi
    constructor(
        public val text: String,
        public val source: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SearchQuery) return false
            return text == other.text &&
                source == other.source
        }

        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + source.hashCode()
            return result
        }

        override fun toString(): String = "SearchQuery(text=$text, source=$source)"
    }
