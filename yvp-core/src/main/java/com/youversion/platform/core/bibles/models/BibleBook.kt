package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class BibleBook(
    @SerialName(CodingKey.ID) val usfm: String?,
    @SerialName(CodingKey.TITLE) val title: String?,
    @SerialName(CodingKey.ABBREVIATION) val abbreviation: String?,
    @SerialName(CodingKey.CANON) val canon: String?,
    @Transient val chapters: List<BibleChapter>? = null,
) {
    object CodingKey {
        const val ID = "id"
        const val TITLE = "title"
        const val ABBREVIATION = "abbreviation"
        const val CANON = "canon"
    }
}
