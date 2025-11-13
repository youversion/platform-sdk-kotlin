package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleVersionIndexChapter(
    @SerialName(CodingKey.ID) val id: String?,
    @SerialName(CodingKey.TITLE) val title: String?,
    @SerialName(CodingKey.VERSES) val verses: List<BibleVersionIndexVerse>?,
) {
    object CodingKey {
        const val ID = "id"
        const val TITLE = "title"
        const val VERSES = "verses"
    }
}
