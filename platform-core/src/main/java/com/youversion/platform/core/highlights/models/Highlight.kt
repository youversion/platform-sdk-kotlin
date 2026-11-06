package com.youversion.platform.core.highlights.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class Highlight
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.ID) public val id: String? = null,
        @SerialName(CodingKey.BIBLE_ID) public val bibleId: Int,
        @SerialName(CodingKey.PASSAGE_ID) public val passageId: String,
        @SerialName(CodingKey.COLOR) public val color: String,
        @SerialName(CodingKey.USER_ID) public val userId: String? = null,
        @SerialName(CodingKey.CREATE_TIME) public val createTime: String? = null,
        @SerialName(CodingKey.UPDATE_TIME) public val updateTime: String? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Highlight) return false
            return id == other.id &&
                bibleId == other.bibleId &&
                passageId == other.passageId &&
                color == other.color &&
                userId == other.userId &&
                createTime == other.createTime &&
                updateTime == other.updateTime
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + bibleId.hashCode()
            result = 31 * result + passageId.hashCode()
            result = 31 * result + color.hashCode()
            result = 31 * result + userId.hashCode()
            result = 31 * result + createTime.hashCode()
            result = 31 * result + updateTime.hashCode()
            return result
        }

        override fun toString(): String =
            "Highlight(id=$id, bibleId=$bibleId, passageId=$passageId, color=$color, userId=$userId, " +
                "createTime=$createTime, updateTime=$updateTime)"

        private object CodingKey {
            const val ID = "id"
            const val BIBLE_ID = "bible_id"
            const val PASSAGE_ID = "passage_id"
            const val COLOR = "color"
            const val USER_ID = "user_id"
            const val CREATE_TIME = "create_time"
            const val UPDATE_TIME = "update_time"
        }
    }
