package com.youversion.platform.ui.views

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Renders the limited Markdown the sign-in prompt copy uses — `**bold**` and nothing else — as an [AnnotatedString].
 *
 * Text between paired `**` is bold; everything else is plain. Copy with no markers renders as-is, so a string that
 * has not yet been given emphasis still displays correctly. Kept deliberately small: the strings it renders are the
 * SDK's own, not arbitrary user input, so it does not try to cover the rest of Markdown.
 */
internal fun boldMarkdownAnnotatedString(source: String): AnnotatedString =
    buildAnnotatedString {
        source.split("**").forEachIndexed { index, segment ->
            // Segments at odd indices sat between a pair of "**" delimiters, so they are the emphasized runs.
            if (index % 2 == 1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(segment) }
            } else {
                append(segment)
            }
        }
    }
