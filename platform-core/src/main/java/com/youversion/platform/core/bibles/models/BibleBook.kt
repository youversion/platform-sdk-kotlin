package com.youversion.platform.core.bibles.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class BibleBook
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.ID) public val id: String?,
        @SerialName(CodingKey.TITLE) public val title: String?,
        @SerialName(CodingKey.FULL_TITLE) public val fullTitle: String?,
        @SerialName(CodingKey.ABBREVIATION) public val abbreviation: String?,
        @SerialName(CodingKey.CANON) public val canon: String?,
        @SerialName(CodingKey.CHAPTERS) public val chapters: List<BibleChapter>?,
        @SerialName(CodingKey.INTRO) public val intro: BibleBookIntro? = null,
    ) {
        public val usfm: String?
            get() = id

        public val isCanonical: Boolean
            get() = canon == "old_testament" || canon == "new_testament"

        public val hasIntro: Boolean
            get() = intro != null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BibleBook) return false
            return id == other.id &&
                title == other.title &&
                fullTitle == other.fullTitle &&
                abbreviation == other.abbreviation &&
                canon == other.canon &&
                chapters == other.chapters &&
                intro == other.intro
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + fullTitle.hashCode()
            result = 31 * result + abbreviation.hashCode()
            result = 31 * result + canon.hashCode()
            result = 31 * result + chapters.hashCode()
            result = 31 * result + intro.hashCode()
            return result
        }

        override fun toString(): String =
            "BibleBook(id=$id, title=$title, fullTitle=$fullTitle, abbreviation=$abbreviation, canon=$canon, " +
                "chapters=$chapters, intro=$intro)"

        public object CodingKey {
            public const val ID: String = "id"
            public const val TITLE: String = "title"
            public const val FULL_TITLE: String = "full_title"
            public const val ABBREVIATION: String = "abbreviation"
            public const val CANON: String = "canon"
            public const val CHAPTERS: String = "chapters"
            public const val INTRO: String = "intro"
        }
    }
