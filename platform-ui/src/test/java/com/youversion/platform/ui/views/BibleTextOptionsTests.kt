package com.youversion.platform.ui.views

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BibleTextOptionsTests {
    private fun options(
        fontFamily: FontFamily = FontFamily.Serif,
        fontSize: TextUnit = 16.sp,
        lineSpacingFraction: Float? = 0.5f,
        paragraphSpacing: TextUnit? = 4.sp,
        textColor: Color? = Color.Red,
        wocColor: Color = Color.Blue,
        renderHeadlines: Boolean = true,
        renderVerseNumbers: Boolean = true,
        footnoteMode: BibleTextFootnoteMode = BibleTextFootnoteMode.NONE,
        footnoteMarker: AnnotatedString? = AnnotatedString("*"),
        selectionColor: Color? = Color.Green,
    ) = BibleTextOptions(
        fontFamily = fontFamily,
        fontSize = fontSize,
        lineSpacingFraction = lineSpacingFraction,
        paragraphSpacing = paragraphSpacing,
        textColor = textColor,
        wocColor = wocColor,
        renderHeadlines = renderHeadlines,
        renderVerseNumbers = renderVerseNumbers,
        footnoteMode = footnoteMode,
        footnoteMarker = footnoteMarker,
        selectionColor = selectionColor,
    )

    @Test
    fun `values with the same fields are equal and hash alike`() {
        assertEquals(options(), options())
        assertEquals(options().hashCode(), options().hashCode())
    }

    @Test
    fun `a difference in any field makes values unequal`() {
        listOf(
            options(fontFamily = FontFamily.Monospace),
            options(fontSize = 18.sp),
            options(lineSpacingFraction = null),
            options(paragraphSpacing = null),
            options(textColor = null),
            options(wocColor = Color.Unspecified),
            options(renderHeadlines = false),
            options(renderVerseNumbers = false),
            options(footnoteMode = BibleTextFootnoteMode.INLINE),
            options(footnoteMarker = null),
            options(selectionColor = null),
        ).forEach {
            assertNotEquals(options(), it)
            assertNotEquals(options().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `line spacing compares by value so equal options always hash alike`() {
        assertEquals(options(lineSpacingFraction = Float.NaN), options(lineSpacingFraction = Float.NaN))
        assertNotEquals(options(lineSpacingFraction = 0f), options(lineSpacingFraction = -0f))
    }

    @Test
    fun `toString matches the existing format`() {
        assertEquals(
            "BibleTextOptions(fontFamily=${FontFamily.Serif}, fontSize=${16.sp}, lineSpacingFraction=0.5, " +
                "paragraphSpacing=${4.sp}, textColor=${Color.Red}, wocColor=${Color.Blue}, renderHeadlines=true, " +
                "renderVerseNumbers=true, footnoteMode=${BibleTextFootnoteMode.NONE}, footnoteMarker=*, " +
                "selectionColor=${Color.Green})",
            options().toString(),
        )
    }
}
