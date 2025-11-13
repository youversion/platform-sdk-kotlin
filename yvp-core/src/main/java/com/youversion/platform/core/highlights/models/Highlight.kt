package com.youversion.platform.core.highlights.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Highlight(
    @SerialName(CodingKey.ID) val id: String? = null,
    @SerialName(CodingKey.VERSION_ID) val versionId: Int,
    @SerialName(CodingKey.PASSAGE_ID) val passageId: String,
    @SerialName(CodingKey.COLOR) val color: String,
    @SerialName(CodingKey.USER_ID) val userId: String? = null,
    @SerialName(CodingKey.CREATE_TIME) val createTime: String? = null,
    @SerialName(CodingKey.UPDATE_TIME) val updateTime: String? = null,
) {
    private object CodingKey {
        const val ID = "id"
        const val VERSION_ID = "version_id"
        const val PASSAGE_ID = "passage_id"
        const val COLOR = "color"
        const val USER_ID = "user_id"
        const val CREATE_TIME = "create_time"
        const val UPDATE_TIME = "update_time"
    }
}
