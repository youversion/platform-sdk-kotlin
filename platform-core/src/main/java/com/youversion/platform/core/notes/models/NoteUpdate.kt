package com.youversion.platform.core.notes.models

import com.youversion.platform.core.bibles.domain.BibleReference

/**
 * Changes some of a note's fields with
 * [NotesApi.updateNote][com.youversion.platform.core.notes.api.NotesApi.updateNote] and leaves the rest as they are.
 *
 * Each field that can be removed takes a [Field]:
 * - `null`, the default, leaves the field unchanged.
 * - [Field.Clear] removes the field's value.
 * - [Field.Value] replaces the field's value.
 *
 * This update removes the title and replaces the tags, and leaves every other field unchanged:
 * ```
 * NoteUpdate(title = NoteUpdate.Field.Clear, tags = NoteUpdate.Field.Value(listOf("faith", "hope")))
 * ```
 * `title = Field.Value("")` would set an empty title rather than remove it. [content] and [status] cannot be
 * removed, so they take a plain value or `null` to leave them unchanged. An update must change at least one field.
 *
 * The server checks the formats of [color], [languageTag] and [tags] and rejects a value that does not match with
 * [REQUEST_REJECTED][com.youversion.platform.core.api.YouVersionNetworkException.Reason.REQUEST_REJECTED].
 *
 * @property title The note's title.
 * @property content The note's text. Cannot be empty.
 * @property color Six lowercase hexadecimal digits without a leading `#`, such as `ffcc00`.
 * @property references The verses the note is attached to, all in one version, book and chapter. A whole-chapter
 *     reference cannot be attached. Use [Field.Clear] rather than an empty list to remove the attachment.
 * @property languageTag The language of the note's text: two lowercase letters for the language, optionally followed by `-` and two uppercase letters for the region, such as `en` or `en-US`.
 * @property tags The note's tags. Each must be nonempty and contain no commas.
 * @property status Who can see the note.
 */
public class NoteUpdate(
    public val title: Field<String>? = null,
    public val content: String? = null,
    public val color: Field<String>? = null,
    public val references: Field<List<BibleReference>>? = null,
    public val languageTag: Field<String>? = null,
    public val tags: Field<List<String>>? = null,
    public val status: NoteStatus? = null,
) {
    /** A change to one field of a note: either [Clear] or a replacement [Value]. */
    public sealed interface Field<out T> {
        /** Removes the field's value. */
        public object Clear : Field<Nothing> {
            override fun toString(): String = "Clear"
        }

        /**
         * Replaces the field's value.
         *
         * @property value The new value.
         */
        public class Value<out T>(
            public val value: T,
        ) : Field<T> {
            override fun equals(other: Any?): Boolean = other is Value<*> && value == other.value

            override fun hashCode(): Int = value.hashCode()

            override fun toString(): String = "Value($value)"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NoteUpdate) return false
        return title == other.title &&
            content == other.content &&
            color == other.color &&
            references == other.references &&
            languageTag == other.languageTag &&
            tags == other.tags &&
            status == other.status
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + references.hashCode()
        result = 31 * result + languageTag.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }

    override fun toString(): String =
        "NoteUpdate(title=${title.redacted()}, content=${content.redacted()}, color=$color, " +
            "references=$references, languageTag=$languageTag, tags=${tags.redacted()}, status=$status)"
}
