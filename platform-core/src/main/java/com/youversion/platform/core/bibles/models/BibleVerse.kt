package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleVerse(
    @SerialName(CodingKey.ID) val id: String?,
    @SerialName(CodingKey.REFERENCE) val reference: String?,
    @SerialName(CodingKey.BOOK_ID) val bookId: String?,
    @SerialName(CodingKey.CHAPTER_ID) val chapterId: String?,
    @SerialName(CodingKey.PASSAGE_ID) val passageId: String?,
) {
    private object CodingKey {
        const val ID = "id"
        const val REFERENCE = "reference"
        const val BOOK_ID = "book_id"
        const val CHAPTER_ID = "chapter_id"
        const val PASSAGE_ID = "passage_id"
    }
}
