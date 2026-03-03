package com.youversion.platform.ui.views

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertNotNull

class BibleTextFontsTests {
    private val fonts = BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 16.sp)

    @Test
    fun `styleFor returns a SpanStyle for every BibleTextFontOption`() {
        BibleTextFontOption.entries.forEach { option ->
            assertNotNull(fonts.styleFor(option), "Expected a non-null SpanStyle for option: $option")
        }
    }
}
