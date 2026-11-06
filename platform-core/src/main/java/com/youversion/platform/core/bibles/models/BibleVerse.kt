package com.youversion.platform.core.bibles.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class BibleVerse
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.ID) public val id: String?,
        @SerialName(CodingKey.PASSAGE_ID) public val passageId: String?,
        @SerialName(CodingKey.TITLE) public val title: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BibleVerse) return false
            return id == other.id &&
                passageId == other.passageId &&
                title == other.title
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + passageId.hashCode()
            result = 31 * result + title.hashCode()
            return result
        }

        override fun toString(): String = "BibleVerse(id=$id, passageId=$passageId, title=$title)"

        private object CodingKey {
            const val ID = "id"
            const val PASSAGE_ID = "passage_id"
            const val TITLE = "title"
        }
    }
