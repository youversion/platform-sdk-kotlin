package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class BibleChapter(
    @SerialName(CodingKey.ID) val id: String?,
    @SerialName(CodingKey.BOOK_USFM) val bookUSFM: String?,
    @Transient val isCanonical: Boolean? = null,
    @SerialName(CodingKey.PASSAGE_ID) val passageId: String?,
    @SerialName(CodingKey.TITLE) val title: String?,
    @Transient val verses: List<BibleVerse>? = null,
) {
    object CodingKey {
        const val ID = "id"
        const val BOOK_USFM = "book_id"
        const val PASSAGE_ID = "passage_id"
        const val TITLE = "title"
    }
}
