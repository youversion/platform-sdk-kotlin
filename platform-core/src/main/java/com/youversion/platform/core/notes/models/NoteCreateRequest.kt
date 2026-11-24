package com.youversion.platform.core.notes.models

import com.youversion.platform.core.bibles.domain.BibleReference
import java.util.UUID

/**
 * A note to create with [NotesApi.createNote][com.youversion.platform.core.notes.api.NotesApi.createNote].
 *
 * Retry a failed create with the same request so that it keeps its [requestId]; the server then creates the note at
 * most once.
 *
 * The server checks the formats of [color], [languageTag] and [tags] and rejects a value that does not match with
 * [REQUEST_REJECTED][com.youversion.platform.core.api.YouVersionNetworkException.Reason.REQUEST_REJECTED].
 *
 * @property content The note's text. May be empty to save the attached passage without text.
 * @property status Who can see the note.
 * @property requestId Identifies this create to the server so that retries do not create duplicates.
 * @property title The note's title.
 * @property color Six lowercase hexadecimal digits without a leading `#`, such as `ffcc00`.
 * @property references The verses to attach, all in one version, book and chapter. Gaps are kept and adjacent or
 *     overlapping verses are merged. A whole-chapter reference cannot be attached. `null` creates a note with no
 *     attachment.
 * @property languageTag The BCP 47 language tag of [content], such as `en-US`.
 * @property tags The note's tags. Each must be nonempty and contain no commas.
 */
public class NoteCreateRequest(
    public val content: String,
    public val status: NoteStatus,
    public val requestId: String = UUID.randomUUID().toString(),
    public val title: String? = null,
    public val color: String? = null,
    public val references: List<BibleReference>? = null,
    public val languageTag: String? = null,
    public val tags: List<String>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NoteCreateRequest) return false
        return content == other.content &&
            status == other.status &&
            requestId == other.requestId &&
            title == other.title &&
            color == other.color &&
            references == other.references &&
            languageTag == other.languageTag &&
            tags == other.tags
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + requestId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + references.hashCode()
        result = 31 * result + languageTag.hashCode()
        result = 31 * result + tags.hashCode()
        return result
    }

    override fun toString(): String =
        "NoteCreateRequest(content=$content, status=$status, requestId=$requestId, title=$title, color=$color, " +
            "references=$references, languageTag=$languageTag, tags=$tags)"
}
