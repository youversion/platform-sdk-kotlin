package com.youversion.platform.reader.domain

import com.youversion.platform.core.bibles.domain.BibleReference
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
 * @property content The note's text; empty saves the passage alone.
 * @property isPublic Whether anyone can see the note rather than only its author.
 * @property hexColor The note's color as six lowercase hexadecimal digits, or `null` for none.
 * @property requestId The create's request id.
 * @property isSaving Whether a save is in flight.
 * @property showsSaveError Whether the last save failed and the reader has yet to acknowledge it.
 * @property showsDiscardConfirmation Whether the reader is being asked to discard unsaved changes.
 */
internal data class NoteDraft(
    val references: List<BibleReference>,
    val selection: Set<BibleReference>,
    val referenceTitle: String,
    val sessionId: String?,
    val content: String = "",
    val isPublic: Boolean = false,
    val hexColor: String? = null,
    val requestId: String = UUID.randomUUID().toString(),
    val isSaving: Boolean = false,
    val showsSaveError: Boolean = false,
    val showsDiscardConfirmation: Boolean = false,
) {
    /** Whether the text, visibility, or color differs from a new, empty note. */
    val hasChanges: Boolean
        get() = content.isNotEmpty() || isPublic || hexColor != null

    /** This draft with the given note fields, under a new [requestId] when any of them changed. */
    fun edited(
        content: String = this.content,
        isPublic: Boolean = this.isPublic,
        hexColor: String? = this.hexColor,
    ): NoteDraft {
        if (content == this.content && isPublic == this.isPublic && hexColor == this.hexColor) return this
        return copy(
            content = content,
            isPublic = isPublic,
            hexColor = hexColor,
            requestId = UUID.randomUUID().toString(),
        )
    }
}
