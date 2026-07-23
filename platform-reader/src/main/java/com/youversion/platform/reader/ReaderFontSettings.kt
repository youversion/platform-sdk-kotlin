package com.youversion.platform.reader

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.youversion.platform.ui.theme.UntitledSerif

/**
 * A utility object that provides constants and helper functions for managing font settings
 * within the reader feature.
 */
object ReaderFontSettings {
    val availableSizes = listOf(9.sp, 12.sp, 15.sp, 18.sp, 21.sp, 24.sp)
    val DEFAULT_FONT_SIZE: TextUnit = 18.sp

    /**
     * Multipliers applied to the current font size to produce the effective line height.
     * Ordered smallest → largest so [nextLineSpacing] can advance by choosing the next
     * value greater than the current selection.
     */
    val availableLineSpacings: List<Float> = listOf(1.2f, 1.5f, 1.8f)
    const val DEFAULT_LINE_SPACING: Float = 1.5f

    val DEFAULT_FONT_DEFINITION: FontDefinition = FontDefinition("Untitled Serif", UntitledSerif)

    val defaultFontDefinitions: List<FontDefinition> =
        listOf(
            FontDefinition("Untitled Serif", UntitledSerif),
            FontDefinition("Serif", FontFamily.Serif),
            FontDefinition("System Default", FontFamily.Default),
            FontDefinition("Cursive", FontFamily.Cursive),
            FontDefinition("Sans Serif", FontFamily.SansSerif),
            FontDefinition("Monospace", FontFamily.Monospace),
        )

    /**
     * Finds the next available font size smaller than the current one.
     * @param currentSize The current font size as an Int.
     * @return The next smaller size, or first value if the current size is the smallest.
     */
    fun nextSmallerFontSize(currentSize: TextUnit): TextUnit =
        availableSizes.lastOrNull { it < currentSize } ?: availableSizes.first()

    /**
     * Finds the next available font size larger than the current one.
     * @param currentSize The current font size as an Int.
     * @return The next larger size, or last value if the current size is the largest.
     */
    fun nextLargerFontSize(currentSize: TextUnit): TextUnit =
        availableSizes.firstOrNull { it > currentSize } ?: availableSizes.last()

    /**
     * Advances to the next line-spacing multiplier, wrapping back to the smallest option
     * when the current selection is already the largest. Mirrors Swift's
     * `ReaderFonts.nextLineSpacing`.
     * @param currentSpacing The current line-spacing multiplier.
     * @return The smallest option greater than [currentSpacing], or the smallest option
     * overall if [currentSpacing] is already the largest (wrap).
     */
    fun nextLineSpacing(currentSpacing: Float): Float =
        availableLineSpacings.firstOrNull { it > currentSpacing } ?: availableLineSpacings.first()
}

data class FontDefinition(
    val fontName: String,
    val fontFamily: FontFamily,
)
