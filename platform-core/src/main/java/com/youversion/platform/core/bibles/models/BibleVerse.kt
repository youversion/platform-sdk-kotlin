package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleVerse(
    @SerialName(CodingKey.ID) val id: String?,
    @SerialName(CodingKey.PASSAGE_ID) val passageId: String?,
    @SerialName(CodingKey.TITLE) val title: String?,
) {
    private object CodingKey {
        const val ID = "id"
        const val PASSAGE_ID = "passage_id"
        const val TITLE = "title"
    }
}
