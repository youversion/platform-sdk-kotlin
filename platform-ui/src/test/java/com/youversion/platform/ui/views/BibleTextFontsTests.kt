package com.youversion.platform.ui.views

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class BibleTextFontsTests {
    private val fonts = BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 16.sp)

    @Test
    fun `styleFor returns a SpanStyle for every BibleTextFontOption`() {
        BibleTextFontOption.entries.forEach { option ->
            assertNotNull(fonts.styleFor(option), "Expected a non-null SpanStyle for option: $option")
        }
    }

    @Test
    fun `values with the same font family and size are equal and hash alike`() {
        val same = BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 16.sp)
        assertEquals(fonts, same)
        assertEquals(fonts.hashCode(), same.hashCode())
    }

    @Test
    fun `a different font family or size makes values unequal`() {
        listOf(
            BibleTextFonts(fontFamily = FontFamily.Serif, baseSize = 16.sp),
            BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 18.sp),
        ).forEach {
            assertNotEquals(fonts, it)
            assertNotEquals(fonts.hashCode(), it.hashCode())
        }
    }

    @Test
    fun `toString matches the existing format`() {
        assertEquals(
            "BibleTextFonts(fontFamily=${FontFamily.Default}, baseSize=${16.sp})",
            fonts.toString(),
        )
    }
}
