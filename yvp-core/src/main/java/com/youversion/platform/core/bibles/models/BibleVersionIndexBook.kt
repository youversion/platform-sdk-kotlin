package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleVersionIndexBook(
    @SerialName(CodingKey.ID) val id: String?,
    @SerialName(CodingKey.TITLE) val title: String?,
    @SerialName(CodingKey.FULL_TITLE) val fullTitle: String?,
    @SerialName(CodingKey.ABBREVIATION) val abbreviation: String?,
    @SerialName(CodingKey.CANON) val canon: String?,
    @SerialName(CodingKey.CHAPTERS) val chapters: List<BibleVersionIndexChapter>?,
) {
    object CodingKey {
        const val ID = "id"
        const val TITLE = "title"
        const val FULL_TITLE = "full_title"
        const val ABBREVIATION = "abbreviation"
        const val CANON = "canon"
        const val CHAPTERS = "chapters"
    }
}
