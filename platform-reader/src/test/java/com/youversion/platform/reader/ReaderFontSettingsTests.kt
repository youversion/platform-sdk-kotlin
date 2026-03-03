package com.youversion.platform.reader

import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderFontSettingsTests {
    // availableSizes = [9.sp, 12.sp, 15.sp, 18.sp, 21.sp, 24.sp]

    @Test
    fun `nextSmallerFontSize returns next smaller size for a mid-range value`() {
        assertEquals(15.sp, ReaderFontSettings.nextSmallerFontSize(18.sp))
    }

    @Test
    fun `nextSmallerFontSize clamps to the smallest size when already at minimum`() {
        assertEquals(9.sp, ReaderFontSettings.nextSmallerFontSize(9.sp))
    }

    @Test
    fun `nextSmallerFontSize returns first size when given a value below the minimum`() {
        assertEquals(9.sp, ReaderFontSettings.nextSmallerFontSize(8.sp))
    }

    @Test
    fun `nextSmallerFontSize steps down one from the largest size`() {
        assertEquals(21.sp, ReaderFontSettings.nextSmallerFontSize(24.sp))
    }

    @Test
    fun `nextSmallerFontSize returns the largest available size when given a value above the maximum`() {
        assertEquals(24.sp, ReaderFontSettings.nextSmallerFontSize(30.sp))
    }

    @Test
    fun `nextSmallerFontSize steps down one from the second-smallest size`() {
        assertEquals(9.sp, ReaderFontSettings.nextSmallerFontSize(12.sp))
    }

    @Test
    fun `nextLargerFontSize returns next larger size for a mid-range value`() {
        assertEquals(21.sp, ReaderFontSettings.nextLargerFontSize(18.sp))
    }

    @Test
    fun `nextLargerFontSize clamps to the largest size when already at maximum`() {
        assertEquals(24.sp, ReaderFontSettings.nextLargerFontSize(24.sp))
    }

    @Test
    fun `nextLargerFontSize returns last size when given a value above the maximum`() {
        assertEquals(24.sp, ReaderFontSettings.nextLargerFontSize(30.sp))
    }

    @Test
    fun `nextLargerFontSize steps up one from the smallest size`() {
        assertEquals(12.sp, ReaderFontSettings.nextLargerFontSize(9.sp))
    }

    @Test
    fun `nextLargerFontSize returns the smallest available size when given a value below the minimum`() {
        assertEquals(9.sp, ReaderFontSettings.nextLargerFontSize(8.sp))
    }

    @Test
    fun `nextLargerFontSize steps up one from the second-largest size`() {
        assertEquals(24.sp, ReaderFontSettings.nextLargerFontSize(21.sp))
    }

    // lineSpacingMultiplierOptions = [1.5f, 2.0f, 2.5f]

    @Test
    fun `nextLineSpacingMultiplier advances from the smallest option`() {
        assertEquals(2.0f, ReaderFontSettings.nextLineSpacingMultiplier(1.5f))
    }

    @Test
    fun `nextLineSpacingMultiplier advances from a middle option`() {
        assertEquals(2.5f, ReaderFontSettings.nextLineSpacingMultiplier(2.0f))
    }

    @Test
    fun `nextLineSpacingMultiplier wraps around to the smallest when at the largest option`() {
        assertEquals(1.5f, ReaderFontSettings.nextLineSpacingMultiplier(2.5f))
    }

    @Test
    fun `nextLineSpacingMultiplier returns first option when given a value below all options`() {
        assertEquals(1.5f, ReaderFontSettings.nextLineSpacingMultiplier(1.0f))
    }

    @Test
    fun `nextLineSpacingMultiplier wraps to smallest when given a value above all options`() {
        assertEquals(1.5f, ReaderFontSettings.nextLineSpacingMultiplier(3.0f))
    }

    @Test
    fun `getLineSpacingSettingIndex returns 0 for the first option`() {
        assertEquals(0, ReaderFontSettings.getLineSpacingSettingIndex(1.5f))
    }

    @Test
    fun `getLineSpacingSettingIndex returns 1 for the middle option`() {
        assertEquals(1, ReaderFontSettings.getLineSpacingSettingIndex(2.0f))
    }

    @Test
    fun `getLineSpacingSettingIndex returns 2 for the last option`() {
        assertEquals(2, ReaderFontSettings.getLineSpacingSettingIndex(2.5f))
    }

    @Test
    fun `getLineSpacingSettingIndex returns -1 for a value not in the list`() {
        assertEquals(-1, ReaderFontSettings.getLineSpacingSettingIndex(1.0f))
    }
}
