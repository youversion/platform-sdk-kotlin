package com.youversion.platform.core.bibles.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class BibleBookIntro
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.ID) public val id: String,
        @SerialName(CodingKey.PASSAGE_ID) public val passageId: String,
        @SerialName(CodingKey.TITLE) public val title: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BibleBookIntro) return false
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

        override fun toString(): String = "BibleBookIntro(id=$id, passageId=$passageId, title=$title)"

        public object CodingKey {
            public const val ID: String = "id"
            public const val PASSAGE_ID: String = "passage_id"
            public const val TITLE: String = "title"
        }
    }
