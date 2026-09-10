package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import java.util.UUID

data class BibleTextBlock(
    val id: UUID = UUID.randomUUID(),
    val text: AnnotatedString,
    val chapter: Int,
    val rows: List<List<AnnotatedString>> = emptyList(),
    /**
     * Indent applied to the block's first line only, in units of three non-breaking spaces capped at 24.
     *
     * Swift has no first-line indent on `AttributedString` and fakes it with that many spaces rendered at the
     * body font, so the indent scales with the font size. Compose sizes the real `TextIndent` in `sp` to match,
     * which means it scales with the user's font-scale setting as well.
     */
    val firstLineHeadIndent: Int,
    /**
     * Indent applied to every line of the block, in units of 8dp.
     *
     * Unlike [firstLineHeadIndent] this is a fixed physical size: Swift applies it as leading padding in points,
     * so it tracks neither the font size nor the font-scale setting. The asymmetry is deliberate parity rather
     * than an oversight; changing it here alone would drift Android away from iOS.
     */
    val headIndent: Int,
    val marginTop: Dp,
    val marginBottom: Dp,
    val alignment: TextAlign,
    val footnotes: List<AnnotatedString>,
)

enum class BibleTextCategory {
    SCRIPTURE,
    VERSE_LABEL,
    FOOTNOTE_MARKER,
    FOOTNOTE_IMAGE,
    FOOTNOTE_TEXT,
    HEADER,

    /** A title naming the book itself, as opposed to a heading naming a section within it. */
    BOOK_TITLE,
}

/** Whether the block renders as anything; an empty one is skipped rather than laid out. */
internal val BibleTextBlock.isVisible: Boolean
    get() = text.isNotBlank() || rows.isNotEmpty()

/**
 * Whether the passage opens with a book title of its own, which a host would otherwise duplicate
 * with a heading of its own.
 *
 * Section headings such as an intro's Outline or Introduction do not count: they name a section
 * rather than the book, so a host heading above them is not a duplicate. Blocks that render as
 * nothing are skipped, so a title hidden by
 * [com.youversion.platform.ui.views.BibleTextOptions.renderHeadlines] does not count either.
 */
internal fun List<BibleTextBlock>.hasLeadingBookTitle(): Boolean {
    val leadingBlock = firstOrNull { it.isVisible } ?: return false
    return leadingBlock.rows.isEmpty() &&
        leadingBlock.text
            .getStringAnnotations(
                tag = BibleTextCategoryAttribute.NAME,
                start = 0,
                end = leadingBlock.text.length,
            ).any { it.item == BibleTextCategory.BOOK_TITLE.name }
}
