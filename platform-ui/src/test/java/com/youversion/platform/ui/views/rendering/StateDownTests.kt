package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.youversion.platform.ui.views.BibleTextFontOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StateDownTests {
    @Test
    fun `copy produces independent copy with same values`() {
        val original =
            StateDown(
                woc = true,
                smallcaps = true,
                alignment = TextAlign.End,
                currentFont = BibleTextFontOption.FONT_117EM_500,
                textCategory = BibleTextCategory.HEADER,
                nodeDepth = 3,
                marginTop = 4.dp,
                marginBottom = 8.dp,
            )

        val copy = original.copy()

        assertEquals(original.woc, copy.woc)
        assertEquals(original.smallcaps, copy.smallcaps)
        assertEquals(original.alignment, copy.alignment)
        assertEquals(original.currentFont, copy.currentFont)
        assertEquals(original.textCategory, copy.textCategory)
        assertEquals(original.nodeDepth, copy.nodeDepth)
        assertEquals(original.marginTop, copy.marginTop)
        assertEquals(original.marginBottom, copy.marginBottom)

        copy.woc = false
        copy.smallcaps = false
        copy.currentFont = BibleTextFontOption.FONT_100EM

        assertTrue(original.woc)
        assertTrue(original.smallcaps)
        assertEquals(BibleTextFontOption.FONT_117EM_500, original.currentFont)

        assertNotEquals(original.woc, copy.woc)
        assertNotEquals(original.smallcaps, copy.smallcaps)
        assertNotEquals(original.currentFont, copy.currentFont)
    }
}
