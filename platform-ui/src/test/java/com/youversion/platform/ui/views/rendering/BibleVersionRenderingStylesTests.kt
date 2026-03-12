package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleTextNode
import com.youversion.platform.ui.views.BibleTextFontOption
import com.youversion.platform.ui.views.BibleTextFonts
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleVersionRenderingStylesTests {
    private val fonts = BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 16.sp)
    private val indentStep = TextUnit(fonts.baseSize.value, TextUnitType.Sp)
    private val noIndent = TextUnit(0f, TextUnitType.Sp)

    private fun defaultStateIn(
        fromVerse: Int = 1,
        toVerse: Int = 176,
        renderHeadlines: Boolean = true,
    ): StateIn =
        StateIn(
            versionId = 1,
            bookUSFM = "GEN",
            currentChapter = 1,
            fromVerse = fromVerse,
            toVerse = toVerse,
            renderVerseNumbers = true,
            renderHeadlines = renderHeadlines,
            footnoteMode = BibleTextFootnoteMode.NONE,
            footnoteMarker = null,
            textColor = Color.Black,
            wocColor = Color.Red,
            fonts = fonts,
        )

    private fun defaultStateDown(
        smallcaps: Boolean = false,
        currentFont: BibleTextFontOption = BibleTextFontOption.TEXT,
        textCategory: BibleTextCategory = BibleTextCategory.SCRIPTURE,
    ): StateDown =
        StateDown(
            smallcaps = smallcaps,
            currentFont = currentFont,
            textCategory = textCategory,
        )

    private fun defaultStateUp(): StateUp =
        StateUp(
            rendering = true,
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
            verse = 1,
        )

    private fun node(
        vararg classes: String,
        attributes: Map<String, String> = emptyMap(),
    ): BibleTextNode =
        BibleTextNode(
            name = "span",
            classes = classes.toList(),
            attributes = attributes,
        )

    @Test
    fun `interpretTextAttr sets SMALL_CAPS when smallcaps is already true`() {
        val stateDown = defaultStateDown(smallcaps = true)
        interpretTextAttr(node(), defaultStateIn(), stateDown, defaultStateUp())
        assertEquals(BibleTextFontOption.SMALL_CAPS, stateDown.currentFont)
    }

    @Test
    fun `interpretTextAttr sets woc for wj class`() {
        val stateDown = defaultStateDown()
        interpretTextAttr(node("wj"), defaultStateIn(), stateDown, defaultStateUp())
        assertTrue(stateDown.woc)
    }

    @Test
    fun `interpretTextAttr sets verse and rendering for yv-v class`() {
        val stateUp = defaultStateUp()
        interpretTextAttr(
            node("yv-v", attributes = mapOf("v" to "5")),
            defaultStateIn(fromVerse = 1, toVerse = 10),
            defaultStateDown(),
            stateUp,
        )
        assertEquals(5, stateUp.verse)
        assertTrue(stateUp.rendering)
    }

    @Test
    fun `interpretTextAttr sets rendering false when verse out of range`() {
        val stateUp = defaultStateUp()
        interpretTextAttr(
            node("yv-v", attributes = mapOf("v" to "20")),
            defaultStateIn(fromVerse = 1, toVerse = 10),
            defaultStateDown(),
            stateUp,
        )
        assertEquals(20, stateUp.verse)
        assertFalse(stateUp.rendering)
    }

    @Test
    fun `interpretTextAttr ignores yv-v when v attribute is not a number`() {
        val stateUp = defaultStateUp()
        interpretTextAttr(
            node("yv-v", attributes = mapOf("v" to "abc")),
            defaultStateIn(),
            defaultStateDown(),
            stateUp,
        )
        assertEquals(1, stateUp.verse)
    }

    @Test
    fun `interpretTextAttr sets SMALL_CAPS and smallcaps for nd class`() {
        val stateDown = defaultStateDown()
        interpretTextAttr(node("nd"), defaultStateIn(), stateDown, defaultStateUp())
        assertEquals(BibleTextFontOption.SMALL_CAPS, stateDown.currentFont)
        assertTrue(stateDown.smallcaps)
    }

    @Test
    fun `interpretTextAttr sets TEXT_ITALIC for tl class`() {
        val stateDown = defaultStateDown()
        interpretTextAttr(node("tl"), defaultStateIn(), stateDown, defaultStateUp())
        assertEquals(BibleTextFontOption.TEXT_ITALIC, stateDown.currentFont)
    }

    @Test
    fun `interpretTextAttr sets TEXT_ITALIC for fq class`() {
        val stateDown = defaultStateDown()
        interpretTextAttr(node("fq"), defaultStateIn(), stateDown, defaultStateUp())
        assertEquals(BibleTextFontOption.TEXT_ITALIC, stateDown.currentFont)
    }

    @Test
    fun `interpretTextAttr sets TEXT_ITALIC for qs class`() {
        val stateDown = defaultStateDown()
        interpretTextAttr(node("qs"), defaultStateIn(), stateDown, defaultStateUp())
        assertEquals(BibleTextFontOption.TEXT_ITALIC, stateDown.currentFont)
    }

    @Test
    fun `interpretTextAttr sets VERSE_NUM for ord class`() {
        val stateDown = defaultStateDown()
        interpretTextAttr(node("ord"), defaultStateIn(), stateDown, defaultStateUp())
        assertEquals(BibleTextFontOption.VERSE_NUM, stateDown.currentFont)
    }

    @Test
    fun `interpretTextAttr does not fail for known ignored class`() {
        val stateDown = defaultStateDown()
        interpretTextAttr(node("w"), defaultStateIn(), stateDown, defaultStateUp())
        assertEquals(BibleTextFontOption.TEXT, stateDown.currentFont)
    }

    @Test
    fun `interpretTextAttr does not change font for unknown class`() {
        val stateDown = defaultStateDown()
        interpretTextAttr(node("zzz"), defaultStateIn(), stateDown, defaultStateUp())
        assertEquals(BibleTextFontOption.TEXT, stateDown.currentFont)
    }

    private fun callInterpretBlock(
        classes: List<String>,
        stateIn: StateIn = defaultStateIn(),
        stateDown: StateDown = defaultStateDown(),
        stateUp: StateUp = defaultStateUp(),
    ): Dp? {
        var capturedMargin: Dp? = null
        interpretBlockClasses(classes, stateIn, stateDown, stateUp) { capturedMargin = it }
        return capturedMargin
    }

    @Test
    fun `interpretBlockClasses sets paragraph indent for p class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("p"), stateUp = stateUp)
        assertEquals(indentStep * 2, stateUp.firstLineHeadIndent)
        assertEquals(noIndent, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets no indent for m class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("m"), stateUp = stateUp)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertEquals(noIndent, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets End alignment for pr class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("pr"), stateDown = stateDown)
        assertEquals(TextAlign.End, stateDown.alignment)
    }

    @Test
    fun `interpretBlockClasses sets Center alignment and HEADER for pc class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("pc"), stateDown = stateDown)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertTrue(stateDown.smallcaps)
        assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
    }

    @Test
    fun `interpretBlockClasses sets head indent for mi class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("mi"), stateUp = stateUp)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertEquals(indentStep * 2, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets firstLine indent for pi class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("pi"), stateUp = stateUp)
        assertEquals(indentStep, stateUp.firstLineHeadIndent)
        assertEquals(noIndent, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets indents for pi2 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("pi2"), stateUp = stateUp)
        assertEquals(indentStep, stateUp.firstLineHeadIndent)
        assertEquals(indentStep * 2, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets indents for pi3 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("pi3"), stateUp = stateUp)
        assertEquals(indentStep, stateUp.firstLineHeadIndent)
        assertEquals(indentStep * 3, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets head indent for li1 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("ili"), stateUp = stateUp)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertEquals(indentStep, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets head indent for li2 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("li2"), stateUp = stateUp)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertEquals(indentStep * 2, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets head indent for li3 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("li3"), stateUp = stateUp)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertEquals(indentStep * 3, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets head indent for li4 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("li4"), stateUp = stateUp)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertEquals(indentStep * 4, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets no indent for q1 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("q1"), stateUp = stateUp)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertEquals(noIndent, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets indent for pm class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("pm"), stateUp = stateUp)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertEquals(indentStep * 2, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets HEADER_ITALIC and disables rendering for d class`() {
        val stateDown = defaultStateDown()
        val stateUp = defaultStateUp()
        callInterpretBlock(
            listOf("d"),
            stateIn = defaultStateIn(renderHeadlines = false),
            stateDown = stateDown,
            stateUp = stateUp,
        )
        assertEquals(BibleTextFontOption.HEADER_ITALIC, stateDown.currentFont)
        assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
        assertFalse(stateUp.rendering)
    }

    @Test
    fun `interpretBlockClasses sets TEXT_BOLD and center for iot class`() {
        val stateDown = defaultStateDown()
        val margin = callInterpretBlock(listOf("iot"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.TEXT_BOLD, stateDown.currentFont)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(fonts.baseSize.value.dp / 3, margin)
    }

    @Test
    fun `interpretBlockClasses sets HEADER2 and center for is1 class`() {
        val stateDown = defaultStateDown()
        val margin = callInterpretBlock(listOf("is1"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.HEADER2, stateDown.currentFont)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(fonts.baseSize.value.dp / 2, margin)
    }

    @Test
    fun `interpretBlockClasses sets head indent for io1 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("io1"), stateUp = stateUp)
        assertEquals(indentStep * 2, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets HEADER font and category for imt class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("imt"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.HEADER, stateDown.currentFont)
        assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
        assertEquals(TextAlign.Center, stateDown.alignment)
    }

    @Test
    fun `interpretBlockClasses sets no indent for q2 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("q2"), stateUp = stateUp)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertEquals(noIndent, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets no indent for q3 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("q3"), stateUp = stateUp)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertEquals(noIndent, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets no indent for q4 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("q4"), stateUp = stateUp)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertEquals(noIndent, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets TEXT_BOLD and center for is2 class`() {
        val stateDown = defaultStateDown()
        val margin = callInterpretBlock(listOf("is2"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.TEXT_BOLD, stateDown.currentFont)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(fonts.baseSize.value.dp / 3, margin)
    }

    @Test
    fun `interpretBlockClasses sets head indent for io2 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("io2"), stateUp = stateUp)
        assertEquals(indentStep * 3, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets head indent for io3 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("io3"), stateUp = stateUp)
        assertEquals(indentStep * 4, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets HEADER_ITALIC and center for imt2 class`() {
        val stateDown = defaultStateDown()
        val margin = callInterpretBlock(listOf("imt2"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.HEADER_ITALIC, stateDown.currentFont)
        assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(fonts.baseSize.value.dp / 2, margin)
    }

    @Test
    fun `interpretBlockClasses sets HEADER3 and center for imt3 class`() {
        val stateDown = defaultStateDown()
        val margin = callInterpretBlock(listOf("imt3"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.HEADER3, stateDown.currentFont)
        assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(fonts.baseSize.value.dp / 3, margin)
    }

    @Test
    fun `interpretBlockClasses sets HEADER4 and center for imt4 class`() {
        val stateDown = defaultStateDown()
        val margin = callInterpretBlock(listOf("imt4"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.HEADER4, stateDown.currentFont)
        assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(fonts.baseSize.value.dp / 3, margin)
    }

    @Test
    fun `interpretBlockClasses does not fail for unknown class matching ignored tag`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("s1"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.TEXT, stateDown.currentFont)
    }

    @Test
    fun `interpretBlockClasses does not change state for truly unknown class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("zzz"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.TEXT, stateDown.currentFont)
    }

    @Test
    fun `interpretBlockClasses handles yv-h with s1 font mapping and mr margin reset`() {
        val stateDown = defaultStateDown()
        val stateUp = defaultStateUp()
        val margin =
            callInterpretBlock(
                listOf("yv-h", "s1", "mr"),
                stateIn = defaultStateIn(renderHeadlines = false),
                stateDown = stateDown,
                stateUp = stateUp,
            )
        assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
        assertEquals(BibleTextFontOption.HEADER_SMALLER, stateDown.currentFont)
        assertEquals(0.dp, margin)
        assertEquals(noIndent, stateUp.firstLineHeadIndent)
        assertFalse(stateUp.rendering)
    }

}
