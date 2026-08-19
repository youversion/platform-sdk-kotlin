package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import java.util.UUID

data class BibleTextBlock(
    val id: UUID = UUID.randomUUID(),
    val text: AnnotatedString,
    val chapter: Int,
    val rows: List<List<AnnotatedString>> = emptyList(),
    val headIndent: TextUnit,
    val marginTop: Dp,
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
}

/**
 * Whether the passage opens with a title of its own, rather than with body text.
 *
 * Blocks that render as nothing are skipped, so a title hidden by
 * [com.youversion.platform.ui.views.BibleTextOptions.renderHeadlines] does not count.
 */
internal fun List<BibleTextBlock>.hasLeadingTitle(): Boolean {
    val leadingBlock = firstOrNull { it.text.isNotBlank() || it.rows.isNotEmpty() } ?: return false
    return leadingBlock.rows.isEmpty() &&
        leadingBlock.text
            .getStringAnnotations(
                tag = BibleTextCategoryAttribute.NAME,
                start = 0,
                end = leadingBlock.text.length,
            ).any { it.item == BibleTextCategory.HEADER.name }
}
