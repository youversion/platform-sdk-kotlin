package com.youversion.platform.core.notes.models

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.di.PlatformInternalApi

/**
 * A signed-in user's note.
 *
 * @property id The note's identifier. Opaque.
 * @property title The note's title, or `null` when it has none.
 * @property content The note's text.
 * @property color The note's color as six lowercase hexadecimal digits without a leading `#`, such as `ffcc00`.
 * @property references The verses the note is attached to, all in one version, book and chapter, with adjacent
 *     verses merged and gaps kept. `null` when the note has no attachment or the server sent one the SDK cannot
 *     read, so one malformed attachment does not prevent the note from loading.
 * @property languageTag The language of the note's text: two lowercase letters for the language, optionally followed by `-` and two uppercase letters for the region, such as `en` or `en-US`.
 * @property tags The note's tags.
 * @property status Who can see the note.
 * @property createdTime When the note was created, as the ISO 8601 text the server sent.
 * @property updatedTime When the note was last changed, as the ISO 8601 text the server sent, or `null` when it was
 *     never changed.
 */
public class Note
    @PlatformInternalApi
    constructor(
        public val id: String,
        public val title: String?,
        public val content: String,
        public val color: String?,
        public val references: List<BibleReference>?,
        public val languageTag: String?,
        public val tags: List<String>?,
        public val status: NoteStatus,
        public val createdTime: String?,
        public val updatedTime: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Note) return false
            return id == other.id &&
                title == other.title &&
                content == other.content &&
                color == other.color &&
                references == other.references &&
                languageTag == other.languageTag &&
                tags == other.tags &&
                status == other.status &&
                createdTime == other.createdTime &&
                updatedTime == other.updatedTime
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + title.hashCode()
            result = 31 * result + content.hashCode()
            result = 31 * result + color.hashCode()
            result = 31 * result + references.hashCode()
            result = 31 * result + languageTag.hashCode()
            result = 31 * result + tags.hashCode()
            result = 31 * result + status.hashCode()
            result = 31 * result + createdTime.hashCode()
            result = 31 * result + updatedTime.hashCode()
            return result
        }

        override fun toString(): String =
            "Note(id=$id, title=${title.redacted()}, content=${content.redacted()}, color=$color, " +
                "references=$references, languageTag=$languageTag, tags=${tags.redacted()}, status=$status, " +
                "createdTime=$createdTime, updatedTime=$updatedTime)"
    }

/** Hides the user's writing when a note is logged, leaving `null` as it is. */
internal fun Any?.redacted(): String? = this?.let { "<redacted>" }

/**
 * Who can see a note. Values the server adds later are kept as they are rather than failing, so a `when` over a
 * status needs an `else` branch.
 *
 * @property rawValue The value the server uses, such as `private`.
 */
public class NoteStatus(
    public val rawValue: String,
) {
    override fun equals(other: Any?): Boolean = other is NoteStatus && rawValue == other.rawValue

    override fun hashCode(): Int = rawValue.hashCode()

    override fun toString(): String = rawValue

    public companion object {
        /** Only the note's author can see it. */
        public val PRIVATE: NoteStatus = NoteStatus("private")

        /** Anyone can see the note. */
        public val PUBLIC: NoteStatus = NoteStatus("public")
    }
}
