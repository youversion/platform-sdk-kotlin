package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.bibles.domain.BibleTextNode
import com.youversion.platform.ui.views.BibleTextFontOption

internal fun interpretTextAttr(
    node: BibleTextNode,
    stateIn: StateIn,
    stateDown: StateDown,
    stateUp: StateUp,
) {
    node.classes.forEach { c ->
        when (c) {
            "wj" -> stateDown.woc = true

            "yv-v", "verse" -> {
                node.attributes["v"]?.toIntOrNull()?.let { verseNum ->
                    stateUp.verse = verseNum
                    stateUp.rendering = (verseNum >= stateIn.fromVerse) && (verseNum <= stateIn.toVerse)
                }
            }

            "nd", "sc" -> stateDown.smallcaps = true

            "tl", "it", "add", "em", "fq", "fqa", "qac", "qs", "qt", "bk", "sig", "litl" ->
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_ITALIC

            "rq" -> stateDown.currentFont = BibleTextFontOption.FONT_083EM_ITALIC

            "bd" -> stateDown.currentFont = BibleTextFontOption.FONT_100EM_700

            "pn" -> stateDown.currentFont = BibleTextFontOption.FONT_100EM

            "bdit", "fk", "fl" -> stateDown.currentFont = BibleTextFontOption.FONT_100EM_500_ITALIC

            "ord", "fv", "sup" -> {
                // Superscript, really; same thing in practice.
                stateDown.currentFont = BibleTextFontOption.VERSE_NUM_FONT
                stateDown.baselineShift = stateIn.fonts.verseNumBaselineShift
            }

            else -> {
                if (!listOf(
                        "yv-v",
                        "verse",
                        "yv-vlbl",
                        "vlbl",
                        "yv-n",
                        "f",
                        "fp",
                        "fr",
                        "ft",
                        "w",
                        "ior",
                        "ref",
                        "va",
                        "wg",
                        "wh",
                        "x",
                        "xta",
                    ).contains(c)
                ) {
                    assertionFailed("interpretTextAttr: unexpected ", c)
                }
            }
        }
    }

    // An additional footnote paragraph starts on a new line within the footnote's text.
    if (node.classes.contains("fp") &&
        stateDown.textCategory == BibleTextCategory.FOOTNOTE_TEXT &&
        !stateUp.isTextEmpty()
    ) {
        stateUp.append(
            "\n",
            stateIn.fonts.styleFor(stateDown.currentFont),
            BibleTextCategory.FOOTNOTE_TEXT,
        )
    }
}

internal fun interpretBlockClasses(
    classes: List<String>,
    stateIn: StateIn,
    stateDown: StateDown,
    stateUp: StateUp,
) {
    val fontSize = stateIn.fonts.baseSize.value

    for (c in classes) {
        when (c) {
            "cl" -> {
                stateDown.alignment = TextAlign.Center
                stateDown.currentFont = BibleTextFontOption.FONT_117EM_500
                stateDown.marginBottom = (0.25f * fontSize).dp
                stateDown.marginTop = 0.dp
            }

            "cls" -> stateDown.alignment = TextAlign.End

            "d" -> {
                stateDown.alignment = TextAlign.Center
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_ITALIC
                stateDown.marginTop = (0.60f * fontSize).dp
                stateDown.marginBottom = (1.20f * fontSize).dp
                stateDown.textCategory = BibleTextCategory.HEADER
            }

            "iex" -> {
                stateUp.firstLineHeadIndent = 1
                stateUp.headIndent = 0
            }

            "imq" -> {
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_ITALIC
                stateDown.marginBottom = fontSize.dp
                stateDown.marginTop = fontSize.dp
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 2
            }

            "imt" -> {
                stateDown.alignment = TextAlign.Center
                stateDown.currentFont = BibleTextFontOption.FONT_117EM_500
                stateDown.textCategory = BibleTextCategory.BOOK_TITLE
            }

            "imt1" -> {
                stateDown.textCategory = BibleTextCategory.BOOK_TITLE
                stateDown.currentFont = BibleTextFontOption.FONT_117EM_700
                stateDown.alignment = TextAlign.Center
                stateDown.marginTop = fontSize.dp
                stateDown.marginBottom = (0.25f * fontSize).dp
            }

            "imt2" -> {
                stateDown.textCategory = BibleTextCategory.BOOK_TITLE
                stateDown.currentFont = BibleTextFontOption.FONT_108EM_ITALIC
                stateDown.alignment = TextAlign.Center
                stateDown.marginTop = (fontSize / 2).dp
                stateDown.marginBottom = (0.25f * fontSize).dp
            }

            "imt3" -> {
                stateDown.textCategory = BibleTextCategory.BOOK_TITLE
                stateDown.currentFont = BibleTextFontOption.FONT_100EM
                stateDown.alignment = TextAlign.Center
                stateDown.marginTop = (0.15f * fontSize).dp
                stateDown.marginBottom = (0.15f * fontSize).dp
            }

            "ior" -> {}

            "is" -> {
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_500
                stateDown.alignment = TextAlign.Center
                stateDown.marginTop = (fontSize / 2).dp
            }

            "is1" -> {
                stateDown.currentFont = BibleTextFontOption.FONT_117EM_700
                stateDown.alignment = TextAlign.Center
                stateDown.marginTop = (fontSize / 2).dp
                stateDown.marginBottom = (0.50f * fontSize).dp
            }

            "lh" -> {
                stateDown.marginTop = (0.50f * fontSize).dp
                stateUp.firstLineHeadIndent = 1
            }

            "li" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 2
            }

            "li1", "ili", "ili1" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 2
            }

            "li2", "ili2" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 4
            }

            "li3", "ili3" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 6
            }

            "li4", "ili4" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 8
            }

            "lim" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 2
            }

            "m", "im" -> {
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 0
            }

            "mi" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 2
            }

            "mr" -> {
                stateDown.alignment = TextAlign.Center
                stateDown.currentFont = BibleTextFontOption.FONT_117EM_500_ITALIC
                stateDown.marginBottom = (0.60f * fontSize).dp
                stateDown.marginTop = 0.dp
            }

            "ms" -> {
                stateDown.alignment = TextAlign.Center
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_500
                stateDown.marginBottom = (0.60f * fontSize).dp
                stateDown.marginTop = 0.dp
            }

            "ms1" -> {
                stateDown.alignment = TextAlign.Center
                stateDown.currentFont = BibleTextFontOption.FONT_117EM_500
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
            }

            "ms2", "ms3", "ms4" -> {
                stateDown.alignment = TextAlign.Center
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_500
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
            }

            "mt1" -> {
                stateDown.textCategory = BibleTextCategory.BOOK_TITLE
                stateDown.currentFont = BibleTextFontOption.FONT_117EM_500
                stateDown.alignment = TextAlign.Center
                stateDown.marginTop = (0.25f * fontSize).dp
                stateDown.marginBottom = (0.50f * fontSize).dp
            }

            "mt2" -> {
                stateDown.textCategory = BibleTextCategory.BOOK_TITLE
                stateDown.currentFont = BibleTextFontOption.FONT_117EM_500_ITALIC
                stateDown.alignment = TextAlign.Center
                stateDown.marginBottom = (0.25f * fontSize).dp
            }

            "nb" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 0
            }

            "p", "ip" -> {
                stateDown.marginBottom = (0.60f * fontSize).dp
                stateUp.firstLineHeadIndent = 1
                stateUp.headIndent = 0
            }

            "pc" -> {
                stateDown.alignment = TextAlign.Center
                stateDown.marginBottom = (0.60f * fontSize).dp
                stateDown.smallcaps = true
                stateDown.textCategory = BibleTextCategory.HEADER
            }

            "pi" -> {
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 0
            }

            "pi1", "ipi" -> {
                stateDown.marginBottom = (0.60f * fontSize).dp
                stateUp.firstLineHeadIndent = 1
                stateUp.headIndent = 2
            }

            "pi2" -> {
                stateUp.firstLineHeadIndent = 1
                stateUp.headIndent = 4
            }

            "pi3" -> {
                stateUp.firstLineHeadIndent = 1
                stateUp.headIndent = 6
            }

            "pm", "pmc", "pmo" -> {
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 2
            }

            "pmr" -> {
                stateDown.alignment = TextAlign.End
                stateDown.marginBottom = (0.50f * fontSize).dp
            }

            "po" -> {
                stateDown.marginTop = (0.25f * fontSize).dp
                stateDown.marginBottom = (0.25f * fontSize).dp
                stateUp.firstLineHeadIndent = 1
            }

            "qa" -> {
                stateDown.currentFont = BibleTextFontOption.FONT_117EM_500_ITALIC
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
                stateDown.textCategory = BibleTextCategory.HEADER
                stateUp.headIndent = 0
            }

            "qc" -> {
                stateDown.alignment = TextAlign.Center
                stateDown.marginBottom = 0.dp
                stateDown.marginTop = 0.dp
            }

            "qm" -> {
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 0
            }

            "qm1" -> {
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 2
            }

            "qm2" -> {
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 4
            }

            "qm3" -> {
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 6
            }

            "qm4" -> {
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 8
            }

            "qr" -> {
                stateDown.alignment = TextAlign.End
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_ITALIC
            }

            "q", "q1", "iq", "iq1" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 2
            }

            "q2", "iq2" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 4
            }

            "q3", "iq3" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 6
            }

            "q4", "iq4" -> {
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 8
            }

            "r" -> {
                stateDown.currentFont =
                    if (classes.contains("yv-h")) {
                        BibleTextFontOption.FONT_100EM_500_ITALIC
                    } else {
                        BibleTextFontOption.FONT_100EM_ITALIC
                    }
                stateDown.alignment = TextAlign.Center
                stateDown.marginTop = 0.dp
                stateDown.marginBottom = (0.25f * fontSize).dp
            }

            "s", "s1" -> {
                stateDown.marginTop = 0.dp
                stateDown.marginBottom = (0.25f * fontSize).dp
                stateDown.currentFont = BibleTextFontOption.FONT_117EM_500
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 0
            }

            "s2", "s3", "s4" -> {
                stateDown.marginTop = (0.5f * fontSize).dp
                stateDown.marginBottom = (0.5f * fontSize).dp
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_500_ITALIC
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 0
            }

            "sp" -> {
                stateDown.currentFont = BibleTextFontOption.FONT_117EM_500_ITALIC
                stateDown.marginBottom = (0.50f * fontSize).dp
                stateDown.marginTop = (0.50f * fontSize).dp
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 0
            }

            "sr" -> {
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_500
                stateDown.alignment = TextAlign.Center
                stateDown.marginBottom = (0.25f * fontSize).dp
            }

            "yv-h", "yvh" -> {
                stateUp.firstLineHeadIndent = 0
                stateDown.textCategory =
                    if (classes.any { it.startsWith("imt") || it.startsWith("mt") }) {
                        BibleTextCategory.BOOK_TITLE
                    } else {
                        BibleTextCategory.HEADER
                    }
                if (!stateIn.renderHeadlines) {
                    stateUp.rendering = false
                }
            }

            // The tags below here are not yet adjusted for our new
            // typography standards; they may or may not reflect the new way.
            "imi" -> {
                stateDown.marginBottom = (0.60f * fontSize).dp
                stateUp.firstLineHeadIndent = 1
                stateUp.headIndent = 0
            }

            "pr" -> stateDown.alignment = TextAlign.End

            "iot" -> {
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_500
                stateDown.alignment = TextAlign.Center
                stateDown.marginTop = (fontSize / 3).dp
            }

            "is2" -> {
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_500
                stateDown.alignment = TextAlign.Center
                stateDown.marginTop = (fontSize / 3).dp
                stateUp.firstLineHeadIndent = 0
                stateUp.headIndent = 0
            }

            "io", "io1" -> stateUp.headIndent = 2

            "io2" -> stateUp.headIndent = 3

            "io3", "io4" -> stateUp.headIndent = 4

            "imte", "imte1" -> {
                stateDown.textCategory = BibleTextCategory.BOOK_TITLE
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_500
                stateDown.alignment = TextAlign.Center
            }

            "imte2" -> {
                stateDown.textCategory = BibleTextCategory.BOOK_TITLE
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_ITALIC
                stateDown.alignment = TextAlign.Center
                stateDown.marginTop = (fontSize / 2).dp
                stateDown.marginBottom = (0.25f * fontSize).dp
            }

            "imt4" -> {
                stateDown.textCategory = BibleTextCategory.BOOK_TITLE
                stateDown.currentFont = BibleTextFontOption.FONT_100EM_500
                stateDown.alignment = TextAlign.Center
                stateDown.marginTop = (fontSize / 3).dp
            }

            "b", "lf" -> {}

            else -> assertionFailed("interpretBlockClasses: unexpected class: ", c)
        }

        // Centered text has nowhere to indent from, so any indent a sibling class asked for is dropped.
        if (stateDown.alignment == TextAlign.Center) {
            stateUp.firstLineHeadIndent = 0
            stateUp.headIndent = 0
        }
    }
}
