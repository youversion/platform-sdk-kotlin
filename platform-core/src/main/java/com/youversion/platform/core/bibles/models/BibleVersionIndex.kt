package com.youversion.platform.core.bibles.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class BibleVersionIndex
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.TEXT_DIRECTION) public val textDirection: String?,
        @SerialName(CodingKey.BOOKS) public val books: List<BibleBook>?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BibleVersionIndex) return false
            return textDirection == other.textDirection &&
                books == other.books
        }

        override fun hashCode(): Int {
            var result = textDirection.hashCode()
            result = 31 * result + books.hashCode()
            return result
        }

        override fun toString(): String = "BibleVersionIndex(textDirection=$textDirection, books=$books)"

        public object CodingKey {
            public const val TEXT_DIRECTION: String = "text_direction"
            public const val BOOKS: String = "books"
        }
    }
