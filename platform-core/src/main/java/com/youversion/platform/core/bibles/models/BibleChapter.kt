package com.youversion.platform.core.bibles.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class BibleChapter
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.ID) public val id: String?,
        @SerialName(CodingKey.PASSAGE_ID) public val passageId: String?,
        @SerialName(CodingKey.TITLE) public val title: String?,
        @SerialName(CodingKey.VERSES) public val verses: List<BibleVerse>?,
    ) {
        public val isCanonical: Boolean
            get() = verses?.isNotEmpty() == true

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BibleChapter) return false
            return id == other.id &&
                passageId == other.passageId &&
                title == other.title &&
                verses == other.verses
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + passageId.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + verses.hashCode()
            return result
        }

        override fun toString(): String = "BibleChapter(id=$id, passageId=$passageId, title=$title, verses=$verses)"

        public object CodingKey {
            public const val ID: String = "id"
            public const val PASSAGE_ID: String = "passage_id"
            public const val TITLE: String = "title"
            public const val VERSES: String = "verses"
        }
    }
