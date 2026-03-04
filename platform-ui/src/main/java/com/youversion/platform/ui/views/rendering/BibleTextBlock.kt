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
