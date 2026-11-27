package com.youversion.platform.reader

import androidx.compose.ui.graphics.vector.ImageVector
import com.youversion.platform.core.bibles.domain.BibleReference

/**
 * One action in the [BibleReader]'s verse action sheet.
 *
 * Pass a list of these as `verseActions` to reorder or leave out the built-in actions ([Highlight], [Save], [Copy],
 * [Share])
 * and to add the host app's own. An empty list shows no sheet at all.
 */
public class BibleReaderVerseAction private constructor(
    /** Identifies the action; unique within the list passed to [BibleReader]. */
    public val id: String,
    internal val kind: Kind,
    internal val label: String?,
    internal val icon: ImageVector?,
    internal val onClick: ((List<BibleReference>) -> Unit)?,
) {
    /**
     * Creates a custom action, shown as a button with [icon] and [label].
     *
     * When the button is tapped, the sheet closes and the selection clears, then [onClick] receives the selected
     * verses, sorted and merged into contiguous ranges.
     *
     * @param id Identifies the action; unique within the list passed to [BibleReader].
     * @param label The button's text, already localized by the host app.
     * @param icon The button's icon.
     * @param onClick Called with the selected verses.
     */
    public constructor(
        id: String,
        label: String,
        icon: ImageVector,
        onClick: (List<BibleReference>) -> Unit,
    ) : this(id = id, kind = Kind.CUSTOM, label = label, icon = icon, onClick = onClick)

    private constructor(id: String, kind: Kind) : this(id = id, kind = kind, label = null, icon = null, onClick = null)

    internal enum class Kind { HIGHLIGHT, SAVE, COPY, SHARE, CUSTOM }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BibleReaderVerseAction) return false
        return id == other.id &&
            kind == other.kind &&
            label == other.label &&
            icon == other.icon &&
            onClick == other.onClick
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + onClick.hashCode()
        return result
    }

    override fun toString(): String = "BibleReaderVerseAction(id=$id, label=$label)"

    public companion object {
        /**
         * The highlight color picker. It is left out for a signed-out reader when the host app has disabled
         * sign-in, since highlighting needs an account.
         */
        public val Highlight: BibleReaderVerseAction = BibleReaderVerseAction(id = "highlight", kind = Kind.HIGHLIGHT)

        /**
         * Opens the notes composer for the selected verses, where the reader saves them with optional text, a color,
         * and a visibility. Saving needs an account with the
         * [WRITE_NOTES][com.youversion.platform.core.users.model.YouVersionPermissions.WRITE_NOTES] permission, which
         * the reader asks for by signing in. It is left out when the host app has disabled sign-in and the reader has
         * no such account.
         */
        public val Save: BibleReaderVerseAction = BibleReaderVerseAction(id = "save", kind = Kind.SAVE)

        /** Copies the selected verses. */
        public val Copy: BibleReaderVerseAction = BibleReaderVerseAction(id = "copy", kind = Kind.COPY)

        /** Shares the selected verses. */
        public val Share: BibleReaderVerseAction = BibleReaderVerseAction(id = "share", kind = Kind.SHARE)

        /** The actions the reader shows when the host app passes none: [Highlight], [Save], [Copy], then [Share]. */
        public val defaults: List<BibleReaderVerseAction> = listOf(Highlight, Save, Copy, Share)
    }
}
