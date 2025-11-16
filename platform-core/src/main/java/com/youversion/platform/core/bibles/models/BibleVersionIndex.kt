package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleVersionIndex(
    @SerialName(CodingKey.TEXT_DIRECTION) val textDirection: String?,
    @SerialName(CodingKey.BOOKS) val books: List<BibleVersionIndexBook>?,
) {
    object CodingKey {
        const val TEXT_DIRECTION = "text_direction"
        const val BOOKS = "books"
    }
}
