package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleChapter(
    @SerialName(CodingKey.ID) val id: String?,
    @SerialName(CodingKey.PASSAGE_ID) val passageId: String?,
    @SerialName(CodingKey.TITLE) val title: String?,
    @SerialName(CodingKey.VERSES) val verses: List<BibleVerse>?,
) {
    val isCanonical: Boolean
        get() = verses?.isNotEmpty() == true

    object CodingKey {
        const val ID = "id"
        const val PASSAGE_ID = "passage_id"
        const val TITLE = "title"
        const val VERSES = "verses"
    }
}
