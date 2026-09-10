package com.youversion.platform.ui.views

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

internal enum class BibleTextFontOption {
    FONT_076EM_ITALIC,
    FONT_100EM,
    FONT_100EM_ITALIC,
    FONT_100EM_500,
    FONT_100EM_500_ITALIC,
    FONT_117EM_500,
    FONT_117EM_500_ITALIC,
    FOOTNOTE,
    VERSE_NUM_FONT,
}

/**
 * Generates the Compose `SpanStyle` variations used to render Bible text,
 * derived from a base font family and size.
 */
data class BibleTextFonts(
    val fontFamily: FontFamily,
    val baseSize: TextUnit = 17.sp,
) {
    private val styles: Map<BibleTextFontOption, SpanStyle> =
        mapOf(
            BibleTextFontOption.FONT_076EM_ITALIC to
                SpanStyle(fontFamily = fontFamily, fontSize = baseSize * 0.76, fontStyle = FontStyle.Italic),
            BibleTextFontOption.FONT_100EM to
                SpanStyle(fontFamily = fontFamily, fontSize = baseSize),
            BibleTextFontOption.FONT_100EM_ITALIC to
                SpanStyle(fontFamily = fontFamily, fontSize = baseSize, fontStyle = FontStyle.Italic),
            BibleTextFontOption.FONT_100EM_500 to
                SpanStyle(fontFamily = fontFamily, fontSize = baseSize, fontWeight = FontWeight.Medium),
            BibleTextFontOption.FONT_100EM_500_ITALIC to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                ),
            BibleTextFontOption.FONT_117EM_500 to
                SpanStyle(fontFamily = fontFamily, fontSize = baseSize * 1.17, fontWeight = FontWeight.Medium),
            BibleTextFontOption.FONT_117EM_500_ITALIC to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize * 1.17,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                ),
            BibleTextFontOption.FOOTNOTE to
                SpanStyle(fontFamily = fontFamily, fontSize = baseSize * 0.8),
            BibleTextFontOption.VERSE_NUM_FONT to
                SpanStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = baseSize * 0.65,
                ),
        )

    // Swift shifts verse numbers up by 0.2 × baseSize; Compose's BaselineShift is a
    // fraction of the span's own font size (0.65 × baseSize), hence the ratio.
    internal val verseNumBaselineShift = BaselineShift(0.2f / 0.65f)
    internal val verseNumOpacity = 0.7f

    internal fun styleFor(
        option: BibleTextFontOption,
        inSmallcaps: Boolean = false,
    ): SpanStyle {
        val style = styles[option]!!
        return if (inSmallcaps && style.fontFeatureSettings == null) {
            style.copy(fontFeatureSettings = "smcp on")
        } else {
            style
        }
    }
}
