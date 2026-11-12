package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.youversion.platform.core.di.PlatformInternalApi
import java.util.UUID

public class BibleTextBlock
    @PlatformInternalApi
    constructor(
        public val id: UUID,
        public val text: AnnotatedString,
        public val chapter: Int,
        public val rows: List<List<AnnotatedString>>,
        /**
         * Indent applied to the block's first line only, in units of three non-breaking spaces capped at 24.
         *
         * Swift has no first-line indent on `AttributedString` and fakes it with that many spaces rendered at the
         * body font, so the indent scales with the font size. Compose sizes the real `TextIndent` in `sp` to match,
         * which means it scales with the user's font-scale setting as well.
         */
        public val firstLineHeadIndent: Int,
        /**
         * Indent applied to every line of the block, in units of 8dp.
         *
         * Unlike [firstLineHeadIndent] this is a fixed physical size: Swift applies it as leading padding in points,
         * so it tracks neither the font size nor the font-scale setting. The asymmetry is deliberate parity rather
         * than an oversight; changing it here alone would drift Android away from iOS.
         */
        public val headIndent: Int,
        public val marginTop: Dp,
        public val marginBottom: Dp,
        public val alignment: TextAlign,
        public val footnotes: List<AnnotatedString>,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BibleTextBlock) return false
            return id == other.id &&
                text == other.text &&
                chapter == other.chapter &&
                rows == other.rows &&
                firstLineHeadIndent == other.firstLineHeadIndent &&
                headIndent == other.headIndent &&
                marginTop == other.marginTop &&
                marginBottom == other.marginBottom &&
                alignment == other.alignment &&
                footnotes == other.footnotes
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + text.hashCode()
            result = 31 * result + chapter.hashCode()
            result = 31 * result + rows.hashCode()
            result = 31 * result + firstLineHeadIndent.hashCode()
            result = 31 * result + headIndent.hashCode()
            result = 31 * result + marginTop.hashCode()
            result = 31 * result + marginBottom.hashCode()
            result = 31 * result + alignment.hashCode()
            result = 31 * result + footnotes.hashCode()
            return result
        }

        override fun toString(): String =
            "BibleTextBlock(id=$id, text=$text, chapter=$chapter, rows=$rows, " +
                "firstLineHeadIndent=$firstLineHeadIndent, " +
                "headIndent=$headIndent, marginTop=$marginTop, marginBottom=$marginBottom, alignment=$alignment, " +
                "footnotes=$footnotes)"
    }

@PlatformInternalApi
public enum class BibleTextCategory {
    SCRIPTURE,
    VERSE_LABEL,
    FOOTNOTE_MARKER,
    FOOTNOTE_IMAGE,
    FOOTNOTE_TEXT,
    HEADER,

    /** A title naming the book itself, as opposed to a heading naming a section within it. */
    BOOK_TITLE,
}

internal const val NO_BREAK_SPACE = '\u00A0'

/**
 * Whether the block renders as anything; an empty one is skipped rather than laid out, while a blank line held
 * open by a non-breaking space is kept.
 */
internal val BibleTextBlock.isVisible: Boolean
    get() = text.any { !it.isWhitespace() || it == NO_BREAK_SPACE } || rows.isNotEmpty()

/** Whether the block renders only as a blank line, such as a stanza break. */
internal val BibleTextBlock.isBlankLine: Boolean
    get() = isVisible && rows.isEmpty() && text.isBlank()

/**
 * Whether the passage opens with a book title of its own, which a host would otherwise duplicate
 * with a heading of its own.
 *
 * Section headings such as an intro's Outline or Introduction do not count: they name a section
 * rather than the book, so a host heading above them is not a duplicate. Blocks that render as
 * nothing are skipped, so a title hidden by
 * [com.youversion.platform.ui.views.BibleTextOptions.renderHeadlines] does not count either. Blank lines are
 * skipped too, so a stanza break above the title does not hide it.
 */
internal fun List<BibleTextBlock>.hasLeadingBookTitle(): Boolean {
    val leadingBlock = firstOrNull { it.isVisible && !it.isBlankLine } ?: return false
    return leadingBlock.rows.isEmpty() &&
        leadingBlock.text
            .getStringAnnotations(
                tag = BibleTextCategoryAttribute.NAME,
                start = 0,
                end = leadingBlock.text.length,
            ).any { it.item == BibleTextCategory.BOOK_TITLE.name }
}
