package com.youversion.platform.reader

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A utility object that provides constants and helper functions for managing font settings
 * within the reader feature.
 */
object ReaderFontSettings {
    val availableSizes = listOf(9.sp, 12.sp, 15.sp, 18.sp, 21.sp, 24.sp)
    val lineSpacingMultiplierOptions = listOf(1.75f, 2.25f, 2.75f)

    val DEFAULT_FONT_SIZE: TextUnit = 18.sp
    const val DEFAULT_LINE_SPACING_MULTIPLIER: Float = 2f

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
     * Rotates to the next line spacing option.
     * If the current spacing is the largest, it wraps around to the smallest.
     * @param currentSpacing The current line spacing value.
     * @return The next line spacing multiplier in the rotation.
     */
    fun nextLineSpacingMultiplier(currentSpacing: Float): Float {
        val nextBigger = lineSpacingMultiplierOptions.firstOrNull { it > currentSpacing }
        return nextBigger ?: lineSpacingMultiplierOptions.minOrNull() ?: currentSpacing
    }
}
