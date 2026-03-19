package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.style.TextAlign
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
                currentFont = BibleTextFontOption.HEADER_ITALIC,
                textCategory = BibleTextCategory.HEADER,
                nodeDepth = 3,
            )

        val copy = original.copy()

        assertEquals(original.woc, copy.woc)
        assertEquals(original.smallcaps, copy.smallcaps)
        assertEquals(original.alignment, copy.alignment)
        assertEquals(original.currentFont, copy.currentFont)
        assertEquals(original.textCategory, copy.textCategory)
        assertEquals(original.nodeDepth, copy.nodeDepth)

        copy.woc = false
        copy.smallcaps = false
        copy.currentFont = BibleTextFontOption.TEXT

        assertTrue(original.woc)
        assertTrue(original.smallcaps)
        assertEquals(BibleTextFontOption.HEADER_ITALIC, original.currentFont)

        assertNotEquals(original.woc, copy.woc)
        assertNotEquals(original.smallcaps, copy.smallcaps)
        assertNotEquals(original.currentFont, copy.currentFont)
    }
}
