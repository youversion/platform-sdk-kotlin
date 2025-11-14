package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleBook(
    @SerialName(CodingKey.ID) val usfm: String?,
    @SerialName(CodingKey.TITLE) val title: String?,
    @SerialName(CodingKey.ABBREVIATION) val abbreviation: String?,
    @SerialName(CodingKey.CANON) val canon: String?,
    @SerialName(CodingKey.BIBLE_CHAPTERS) val chapters: List<BibleChapter>? = null,
) {
    object CodingKey {
        const val ID = "id"
        const val TITLE = "title"
        const val ABBREVIATION = "abbreviation"
        const val CANON = "canon"
        const val BIBLE_CHAPTERS = "BibleChapter" // Local for caching
    }
}
