package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.bibles.domain.BibleTextNode
import com.youversion.platform.ui.views.BibleTextFontOption

internal fun interpretTextAttr(
    node: BibleTextNode,
    stateIn: StateIn,
    stateDown: StateDown,
    stateUp: StateUp,
) {
    if (stateDown.smallcaps) {
        stateDown.currentFont = BibleTextFontOption.SMALL_CAPS
    }

    node.classes.forEach { c ->
        when (c) {
            "wj" -> stateDown.woc = true
            "yv-v", "verse" -> {
                node.attributes["v"]?.toIntOrNull()?.let { verseNum ->
                    stateUp.verse = verseNum
                    stateUp.rendering = (verseNum >= stateIn.fromVerse) && (verseNum <= stateIn.toVerse)
                }
            }

            "nd", "sc" -> {
                stateDown.currentFont = BibleTextFontOption.SMALL_CAPS
                stateDown.smallcaps = true
            }

            "tl", "it", "add" -> stateDown.currentFont = BibleTextFontOption.TEXT_ITALIC
            "fq", "fqa", "add" -> stateDown.currentFont = BibleTextFontOption.TEXT_ITALIC
            "qs", "qt" -> stateDown.currentFont = BibleTextFontOption.TEXT_ITALIC
            "ord", "fv", "sup" -> stateDown.currentFont = BibleTextFontOption.VERSE_NUM
            else -> {
                if (!listOf(
                        "yv-v",
                        "verse",
                        "yv-vlbl",
                        "vlbl",
                        "yv-n",
                        "f",
                        "fr",
                        "ft",
                        "qs",
                        "sc",
                        "nd",
                        "cl",
                        "w",
                        "litl",
                        "rq",
                        "x",
                    ).contains(c)
                ) {
                    assertionFailed("interpretTextAttr: unexpected ", c)
                }
            }
        }
    }
}

internal fun interpretBlockClasses(
    classes: List<String>,
    stateIn: StateIn,
    stateDown: StateDown,
    stateUp: StateUp,
    setMarginTop: (Dp) -> Unit,
) {
    var newAlignment = stateDown.alignment
    var newTextCategory = stateDown.textCategory
    var newSmallCaps = stateDown.smallcaps
    var newCurrentFont = stateDown.currentFont

    val indentStep = TextUnit(stateIn.fonts.baseSize.value, TextUnitType.Sp)
    val noIndent = TextUnit(0f, TextUnitType.Sp)

    val ignoredTags =
        setOf(
            "s1",
            "b",
            "lh",
            "li",
            "li1",
            "li2",
            "li3",
            "li4",
            "lf",
            "mr",
            "ms",
            "ms1",
            "ms2",
            "ms3",
            "ms4",
            "s2",
            "s3",
            "s4",
            "sp",
            "iex",
            "ms1",
            "qa",
            "r",
            "sr",
            "po",
            "im",
            "ior",
        )

    for (c in classes) {
        when (c) {
            "p", "ip", "imi", "ipi" -> {
                stateUp.firstLineHeadIndent = indentStep * 2
                stateUp.headIndent = noIndent
            }

            "m", "nb", "im" -> {
                stateUp.firstLineHeadIndent = noIndent
                stateUp.headIndent = noIndent
            }

            "pr", "qr" -> {
                newAlignment = TextAlign.End
            }

            "pc", "qc" -> {
                newAlignment = TextAlign.Center
                newSmallCaps = true
                newTextCategory = BibleTextCategory.HEADER
            }

            "mi" -> {
                stateUp.firstLineHeadIndent = noIndent
                stateUp.headIndent = indentStep.times(2)
            }

            "pi", "pi1" -> {
                stateUp.firstLineHeadIndent = indentStep
                stateUp.headIndent = noIndent
            }

            "pi2" -> {
                stateUp.firstLineHeadIndent = indentStep
                stateUp.headIndent = indentStep * 2
            }

            "pi3" -> {
                stateUp.firstLineHeadIndent = indentStep
                stateUp.headIndent = indentStep.times(3)
            }

            "li1", "ili", "ili1" -> {
                stateUp.firstLineHeadIndent = noIndent
                stateUp.headIndent = indentStep
            }

            "li2", "ili2" -> {
                stateUp.firstLineHeadIndent = noIndent
                stateUp.headIndent = indentStep * 2
            }

            "li3", "ili3" -> {
                stateUp.firstLineHeadIndent = noIndent
                stateUp.headIndent = indentStep * 3
            }

            "li4", "ili4" -> {
                stateUp.firstLineHeadIndent = noIndent
                stateUp.headIndent = indentStep * 4
            }

            "iq", "iq1", "q", "q1", "qm", "qm1" -> {
                stateUp.firstLineHeadIndent = noIndent
                stateUp.headIndent = noIndent
            }

            "iq2", "q2", "qm2" -> {
                stateUp.firstLineHeadIndent = noIndent
                stateUp.headIndent = noIndent
            }

            "iq3", "q3", "qm3" -> {
                stateUp.firstLineHeadIndent = noIndent
                stateUp.headIndent = noIndent
            }

            "iq4", "q4", "qm4" -> {
                stateUp.firstLineHeadIndent = noIndent
                stateUp.headIndent = noIndent
            }

            "pm", "pmo", "pmc", "pmr" -> {
                stateUp.firstLineHeadIndent = noIndent
                stateUp.headIndent = indentStep.times(2)
            }

            "d" -> {
                newCurrentFont = BibleTextFontOption.HEADER_ITALIC
                newTextCategory = BibleTextCategory.HEADER
                if (!stateIn.renderHeadlines) {
                    stateUp.rendering = false
                }
            }

            "iot" -> {
                newCurrentFont = BibleTextFontOption.TEXT_BOLD
                newAlignment = TextAlign.Center
                setMarginTop(stateIn.fonts.baseSize.value.dp / 3)
            }

            "is", "is1" -> {
                newCurrentFont = BibleTextFontOption.HEADER2
                newAlignment = TextAlign.Center
                setMarginTop(stateIn.fonts.baseSize.value.dp / 2)
            }

            "is2" -> {
                newCurrentFont = BibleTextFontOption.TEXT_BOLD
                newAlignment = TextAlign.Center
                setMarginTop(stateIn.fonts.baseSize.value.dp / 3)
            }

            "io", "io1" -> {
                stateUp.headIndent = indentStep * 2
            }

            "io2" -> {
                stateUp.headIndent = indentStep * 3
            }

            "io3", "io4" -> {
                stateUp.headIndent = indentStep * 4
            }

            "imt", "imt1", "imte", "imte1" -> {
                newTextCategory = BibleTextCategory.HEADER
                newCurrentFont = BibleTextFontOption.HEADER
                newAlignment = TextAlign.Center
            }

            "imt2", "imte2" -> {
                newTextCategory = BibleTextCategory.HEADER
                newCurrentFont = BibleTextFontOption.HEADER_ITALIC
                newAlignment = TextAlign.Center
                setMarginTop(stateIn.fonts.baseSize.value.dp / 2)
            }

            "imt3" -> {
                newTextCategory = BibleTextCategory.HEADER
                newCurrentFont = BibleTextFontOption.HEADER3
                newAlignment = TextAlign.Center
                setMarginTop(stateIn.fonts.baseSize.value.dp / 3)
            }

            "imt4" -> {
                newTextCategory = BibleTextCategory.HEADER
                newCurrentFont = BibleTextFontOption.HEADER4
                newAlignment = TextAlign.Center
                setMarginTop(stateIn.fonts.baseSize.value.dp / 3)
            }

            "yv-h", "yvh" -> {
                val fontMap: Map<String, BibleTextFontOption> =
                    mapOf(
                        "s1" to BibleTextFontOption.HEADER_ITALIC,
                        "imt" to BibleTextFontOption.HEADER,
                        "imt1" to BibleTextFontOption.HEADER,
                        "ms" to BibleTextFontOption.HEADER2,
                        "ms1" to BibleTextFontOption.HEADER2,
                        "s2" to BibleTextFontOption.HEADER2,
                        "ms2" to BibleTextFontOption.HEADER2,
                        "imt2" to BibleTextFontOption.HEADER2,
                        "s3" to BibleTextFontOption.HEADER3,
                        "ms3" to BibleTextFontOption.HEADER3,
                        "imt3" to BibleTextFontOption.HEADER3,
                        "s4" to BibleTextFontOption.HEADER4,
                        "ms4" to BibleTextFontOption.HEADER4,
                        "imt4" to BibleTextFontOption.HEADER4,
                        "sp" to BibleTextFontOption.HEADER_ITALIC,
                        "r" to BibleTextFontOption.HEADER_ITALIC,
                        "sr" to BibleTextFontOption.HEADER_ITALIC,
                        "mr" to BibleTextFontOption.HEADER_SMALLER,
                    )
                newTextCategory = BibleTextCategory.HEADER
                setMarginTop(stateIn.fonts.baseSize.value.dp)
                newCurrentFont = BibleTextFontOption.HEADER

                for (c in classes) {
                    fontMap[c]?.let { font -> newCurrentFont = font }
                }

                if (classes.contains("mr")) {
                    setMarginTop(0.dp)
                }

                stateUp.firstLineHeadIndent = noIndent
                if (!stateIn.renderHeadlines) {
                    stateUp.rendering = false
                }
            }

            else -> {
                if (c !in ignoredTags &&
                    !c.startsWith("ms") && !c.startsWith("s")
                ) {
                    assertionFailed("interpreting block classes: unexpected ", c)
                }
            }
        }
    }

    stateDown.apply {
        alignment = newAlignment
        textCategory = newTextCategory
        smallcaps = newSmallCaps
        currentFont = newCurrentFont
    }
}
