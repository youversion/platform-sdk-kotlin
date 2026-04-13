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
}
