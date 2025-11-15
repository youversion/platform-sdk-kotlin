package com.youversion.platform.ui.views

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Defines the different semantic font styles used for rendering Bible text.
 */
enum class BibleTextFontOption {
    TEXT,
    TEXT_ITALIC,
    VERSE_NUM,
    SMALL_CAPS,
    HEADER,
    HEADER_ITALIC,
    HEADER_SMALLER,
    HEADER2,
    HEADER3,
    HEADER4,
    FOOTNOTE,
}

/**
 * A holder for generating Compose `SpanStyle` objects based on a base font family and size.
 *
 * This class is responsible for creating all the font variations needed to render
 * Bible text, such as different weights, styles, and sizes for headers, footnotes, etc.
 *
 * @param fontFamily The base font family to be used for rendering. This should be a
 *   FontFamily object, which you would typically define elsewhere in your app (e.g., in a Theme file).
 * @param baseSize The base font size for standard scripture text.
 */
data class BibleTextFonts(
    val fontFamily: FontFamily,
    val baseSize: TextUnit = 17.sp,
) {
    // A map to cache the generated SpanStyle objects for performance.
    // The logic from the original Swift init is translated here.
    // It initializes the `styles` map with all the required font variations.

    // In Compose, font variations (bold, italic) are handled by the FontFamily itself
    // if it's properly configured with all its variants. We don't need to manually
    // construct family names like in the Swift code.
    private val styles: Map<BibleTextFontOption, SpanStyle> =
        mapOf(
            BibleTextFontOption.TEXT to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize,
                ),
            BibleTextFontOption.TEXT_ITALIC to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize,
                    fontStyle = FontStyle.Italic,
                ),
            BibleTextFontOption.VERSE_NUM to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize * 0.7,
                    // "smcp" enables the small caps font feature.
                    fontFeatureSettings = "c2sc on, smcp on",
                ),
            BibleTextFontOption.SMALL_CAPS to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize,
                    fontFeatureSettings = "c2sc on, smcp on",
                ),
            BibleTextFontOption.HEADER to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize * 1.2,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold, // A good replacement for a separate header font
                ),
            BibleTextFontOption.HEADER_ITALIC to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize * 1.1,
                    fontStyle = FontStyle.Italic,
                ),
            BibleTextFontOption.HEADER_SMALLER to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize * 0.9,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                ),
            BibleTextFontOption.HEADER2 to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize * 1.1,
                    fontWeight = FontWeight.Bold,
                ),
            BibleTextFontOption.HEADER3 to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize * 1.1,
                    fontWeight = FontWeight.SemiBold,
                ),
            BibleTextFontOption.HEADER4 to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize * 1.1,
                    fontWeight = FontWeight.SemiBold,
                ),
            BibleTextFontOption.FOOTNOTE to
                SpanStyle(
                    fontFamily = fontFamily,
                    fontSize = baseSize * 0.8,
                ),
        )

    val verseNumBaselineShift = BaselineShift.Superscript
    val verseNumOpacity = 0.7f

    /**
     * Retrieves the appropriate `SpanStyle` for a given font option.
     *
     * @param option The semantic font style needed.
     * @return The corresponding `SpanStyle` for use with an `AnnotatedString`.
     */
    fun styleFor(option: BibleTextFontOption): SpanStyle {
        // The '!' is safe because the init block guarantees all enum cases are present.
        return styles[option]!!
    }
}
