package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleVersionIndexVerse(
    @SerialName(CodingKey.ID) val id: String?,
    @SerialName(CodingKey.TITLE) val title: String?,
) {
    object CodingKey {
        const val ID = "id"
        const val TITLE = "title"
    }
}
