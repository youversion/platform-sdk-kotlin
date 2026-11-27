package com.youversion.platform.reader.domain

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.models.highlightRgbOrNull
import com.youversion.platform.core.notes.models.Note
import com.youversion.platform.core.notes.models.NoteCreateRequest
import com.youversion.platform.core.notes.models.NoteStatus
import com.youversion.platform.core.notes.models.NoteUpdate
import java.util.UUID

/**
 * A note the reader is composing for a verse selection, owned by the reader rather than the server until it saves.
 *
 * It belongs to the session in [sessionId], so it is dropped rather than saved when another account signs in. A
 * draft captured while signed out has no session yet and takes the one that signs in to continue it.
 *
 * [requestId] identifies the create to the server, so retrying a failed save does not create the note twice. It
 * changes with the text, visibility, or color, since that is a different note.
 *
 * @property references The selected verses, sorted and merged into ranges with gaps kept.
 * @property selection The verses as the reader selected them, restored when composing is abandoned before it opens.
 * @property referenceTitle The passage shown at the top of the composer.
 * @property sessionId The session the draft belongs to, or `null` while signed out.
 * @property savedNotes The notes already attached to the passage, newest first, listed below the composer when it was
 *     opened from a verse indicator.
 * @property editingNote The saved note the composer is editing, or `null` while composing a new one. Its text,
 *     visibility, and color are what [hasChanges] compares against.
 * @property content The note's text; empty saves the passage alone.
 * @property isPublic Whether anyone can see the note rather than only its author.
 * @property hexColor The note's color as six lowercase hexadecimal digits, or `null` for none.
 * @property requestId The create's request id.
 * @property isSaving Whether a save is in flight.
 * @property isDeleting Whether a saved note's deletion is in flight.
 * @property showsSaveError Whether the last save or deletion failed and the reader has yet to acknowledge it.
 * @property showsDiscardConfirmation Whether the reader is being asked to discard unsaved changes.
 */
internal data class NoteDraft(
    val references: List<BibleReference>,
    val selection: Set<BibleReference>,
    val referenceTitle: String,
    val sessionId: String?,
    val savedNotes: List<Note> = emptyList(),
    val editingNote: Note? = null,
    val content: String = "",
    val isPublic: Boolean = false,
    val hexColor: String? = null,
    val requestId: String = UUID.randomUUID().toString(),
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val showsSaveError: Boolean = false,
    val showsDiscardConfirmation: Boolean = false,
) {
    /** Whether a save or deletion is in flight. */
    val isBusy: Boolean
        get() = isSaving || isDeleting

    /** Whether the text differs from [editingNote]'s, or from empty for a new note. */
    val hasTextChanges: Boolean
        get() = content != editingNote?.content.orEmpty()

    /** Whether the visibility differs from [editingNote]'s, or from private for a new note. */
    val hasVisibilityChanges: Boolean
        get() = isPublic != (editingNote?.status == NoteStatus.PUBLIC)

    /** Whether the color differs from [editingNote]'s, or from none for a new note. */
    val hasColorChanges: Boolean
        get() = hexColor != editingNote?.color

    /** Whether the text, visibility, or color differs from [editingNote], or from a new, empty note. */
    val hasChanges: Boolean
        get() = hasTextChanges || hasVisibilityChanges || hasColorChanges

    /**
     * Whether Save can run: always for a new note, which may save the passage alone, and for a saved note only once a
     * field changed, without emptying text the server requires.
     */
    val canSave: Boolean
        get() = !isBusy && (editingNote == null || (hasChanges && (content.isNotEmpty() || !hasTextChanges)))

    /**
     * This draft with the given note fields, under a new [requestId] when any of them changed. A color that matches
     * [editingNote]'s keeps its saved spelling, so choosing it again is not a change.
     */
    fun edited(
        content: String = this.content,
        isPublic: Boolean = this.isPublic,
        hexColor: String? = this.hexColor,
    ): NoteDraft {
        val savedHexColor = editingNote?.color
        val color =
            if (hexColor != null && savedHexColor != null &&
                highlightRgbOrNull(hexColor) == highlightRgbOrNull(savedHexColor)
            ) {
                savedHexColor
            } else {
                hexColor
            }
        if (content == this.content && isPublic == this.isPublic && color == this.hexColor) return this
        return copy(
            content = content,
            isPublic = isPublic,
            hexColor = color,
            requestId = UUID.randomUUID().toString(),
        )
    }

    /** The request that creates this draft as a new note on [references]. */
    fun createRequest(): NoteCreateRequest =
        NoteCreateRequest(
            content = content,
            status = status,
            requestId = requestId,
            color = hexColor,
            references = references,
        )

    /**
     * The update that changes [editingNote] to this draft, naming only the fields that changed. Removing the color
     * clears it explicitly; the passage and every other field stay as saved.
     */
    fun update(): NoteUpdate =
        NoteUpdate(
            content = content.takeIf { hasTextChanges },
            color =
                when {
                    !hasColorChanges -> null
                    hexColor == null -> NoteUpdate.Field.Clear
                    else -> NoteUpdate.Field.Value(hexColor)
                },
            status = status.takeIf { hasVisibilityChanges },
        )

    private val status: NoteStatus
        get() = if (isPublic) NoteStatus.PUBLIC else NoteStatus.PRIVATE

    /** This draft editing [note], with its text, visibility, and color loaded and any unsaved changes replaced. */
    fun editing(note: Note): NoteDraft =
        copy(
            editingNote = note,
            content = note.content,
            isPublic = note.status == NoteStatus.PUBLIC,
            hexColor = note.color,
        )

    /**
     * This draft once [note] is deleted: gone from [savedNotes], and no longer edited. Its fields empty when they were
     * the note's own, but text the reader changed is kept for a new note.
     */
    fun deleting(note: Note): NoteDraft {
        val remaining = copy(savedNotes = savedNotes.filterNot { it.id == note.id })
        if (editingNote?.id != note.id) return remaining
        return if (hasChanges) {
            remaining.copy(editingNote = null)
        } else {
            remaining.copy(editingNote = null, content = "", isPublic = false, hexColor = null)
        }
    }
}
