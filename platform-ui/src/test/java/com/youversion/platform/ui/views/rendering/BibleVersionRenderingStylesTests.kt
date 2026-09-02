package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
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
    private val fontSize = fonts.baseSize.value

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
        currentFont: BibleTextFontOption = BibleTextFontOption.FONT_100EM,
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
    fun `interpretTextAttr sets smallcaps without changing font for nd and sc classes`() {
        listOf("nd", "sc").forEach { className ->
            val stateDown = defaultStateDown()
            interpretTextAttr(node(className), defaultStateIn(), stateDown, defaultStateUp())
            assertTrue(stateDown.smallcaps)
            assertEquals(BibleTextFontOption.FONT_100EM, stateDown.currentFont)
        }
    }

    @Test
    fun `interpretTextAttr sets italic font for italic classes`() {
        listOf("tl", "it", "add", "fq", "fqa", "qs", "qt", "bk").forEach { className ->
            val stateDown = defaultStateDown()
            interpretTextAttr(node(className), defaultStateIn(), stateDown, defaultStateUp())
            assertEquals(
                BibleTextFontOption.FONT_100EM_ITALIC,
                stateDown.currentFont,
                "Expected italic font for class '$className'",
            )
        }
    }

    @Test
    fun `interpretTextAttr sets bold italic font for bdit class`() {
        val stateDown = defaultStateDown()
        interpretTextAttr(node("bdit"), defaultStateIn(), stateDown, defaultStateUp())
        assertEquals(BibleTextFontOption.FONT_100EM_500_ITALIC, stateDown.currentFont)
    }

    @Test
    fun `interpretTextAttr sets verse number font and baseline shift for ord, fv, and sup classes`() {
        listOf("ord", "fv", "sup").forEach { className ->
            val stateDown = defaultStateDown()
            interpretTextAttr(node(className), defaultStateIn(), stateDown, defaultStateUp())
            assertEquals(BibleTextFontOption.VERSE_NUM_FONT, stateDown.currentFont)
            assertEquals(fonts.verseNumBaselineShift, stateDown.baselineShift)
        }
    }

    @Test
    fun `interpretTextAttr does not fail for known ignored class`() {
        val stateDown = defaultStateDown()
        interpretTextAttr(node("w"), defaultStateIn(), stateDown, defaultStateUp())
        assertEquals(BibleTextFontOption.FONT_100EM, stateDown.currentFont)
    }

    @Test
    fun `interpretTextAttr does not change font for unknown class`() {
        val stateDown = defaultStateDown()
        interpretTextAttr(node("zzz"), defaultStateIn(), stateDown, defaultStateUp())
        assertEquals(BibleTextFontOption.FONT_100EM, stateDown.currentFont)
    }

    private fun callInterpretBlock(
        classes: List<String>,
        stateIn: StateIn = defaultStateIn(),
        stateDown: StateDown = defaultStateDown(),
        stateUp: StateUp = defaultStateUp(),
    ) {
        interpretBlockClasses(classes, stateIn, stateDown, stateUp)
    }

    private fun assertIndents(
        stateUp: StateUp,
        className: String,
        firstLineHeadIndent: Int,
        headIndent: Int,
    ) {
        assertEquals(
            firstLineHeadIndent,
            stateUp.firstLineHeadIndent,
            "Expected first line head indent $firstLineHeadIndent for class '$className'",
        )
        assertEquals(
            headIndent,
            stateUp.headIndent,
            "Expected head indent $headIndent for class '$className'",
        )
    }

    @Test
    fun `interpretBlockClasses sets first line indent and margin for p and ip classes`() {
        listOf("p", "ip").forEach { className ->
            val stateDown = defaultStateDown()
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateDown = stateDown, stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 1, headIndent = 0)
            assertEquals((0.60f * fontSize).dp, stateDown.marginBottom)
        }
    }

    @Test
    fun `interpretBlockClasses sets first line indent and margin for imi class`() {
        val stateDown = defaultStateDown()
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("imi"), stateDown = stateDown, stateUp = stateUp)
        assertIndents(stateUp, "imi", firstLineHeadIndent = 1, headIndent = 0)
        assertEquals((0.60f * fontSize).dp, stateDown.marginBottom)
    }

    @Test
    fun `interpretBlockClasses sets no indent and margins for m and im classes`() {
        listOf("m", "im").forEach { className ->
            val stateDown = defaultStateDown()
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateDown = stateDown, stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 0)
            assertEquals((0.50f * fontSize).dp, stateDown.marginTop)
            assertEquals((0.50f * fontSize).dp, stateDown.marginBottom)
        }
    }

    @Test
    fun `interpretBlockClasses sets no indent for nb class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("nb"), stateUp = stateUp)
        assertIndents(stateUp, "nb", firstLineHeadIndent = 0, headIndent = 0)
    }

    @Test
    fun `interpretBlockClasses sets first line indent for iex class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("iex"), stateUp = stateUp)
        assertIndents(stateUp, "iex", firstLineHeadIndent = 1, headIndent = 0)
    }

    @Test
    fun `interpretBlockClasses sets End alignment for pr and qr classes`() {
        listOf("pr", "qr").forEach { className ->
            val stateDown = defaultStateDown()
            callInterpretBlock(listOf(className), stateDown = stateDown)
            assertEquals(TextAlign.End, stateDown.alignment)
        }
    }

    @Test
    fun `interpretBlockClasses sets italic font for qr class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("qr"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.FONT_100EM_ITALIC, stateDown.currentFont)
    }

    @Test
    fun `interpretBlockClasses sets End alignment and margin for pmr class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("pmr"), stateDown = stateDown)
        assertEquals(TextAlign.End, stateDown.alignment)
        assertEquals((0.50f * fontSize).dp, stateDown.marginBottom)
    }

    @Test
    fun `interpretBlockClasses sets Center alignment, smallcaps, and HEADER for pc class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("pc"), stateDown = stateDown)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertTrue(stateDown.smallcaps)
        assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
        assertEquals((0.60f * fontSize).dp, stateDown.marginBottom)
    }

    @Test
    fun `interpretBlockClasses sets Center alignment and no margins for qc class`() {
        val stateDown = defaultStateDown()
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("qc"), stateDown = stateDown, stateUp = stateUp)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(0.dp, stateDown.marginTop)
        assertEquals(0.dp, stateDown.marginBottom)
        assertIndents(stateUp, "qc", firstLineHeadIndent = 0, headIndent = 0)
    }

    @Test
    fun `interpretBlockClasses sets head indent for mi class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("mi"), stateUp = stateUp)
        assertIndents(stateUp, "mi", firstLineHeadIndent = 0, headIndent = 2)
    }

    @Test
    fun `interpretBlockClasses sets no indent and margins for pi class`() {
        val stateDown = defaultStateDown()
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("pi"), stateDown = stateDown, stateUp = stateUp)
        assertIndents(stateUp, "pi", firstLineHeadIndent = 0, headIndent = 0)
        assertEquals((0.50f * fontSize).dp, stateDown.marginTop)
        assertEquals((0.50f * fontSize).dp, stateDown.marginBottom)
    }

    @Test
    fun `interpretBlockClasses sets indents and margin for pi1 and ipi classes`() {
        listOf("pi1", "ipi").forEach { className ->
            val stateDown = defaultStateDown()
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateDown = stateDown, stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 1, headIndent = 2)
            assertEquals((0.60f * fontSize).dp, stateDown.marginBottom)
        }
    }

    @Test
    fun `interpretBlockClasses sets indents for pi2 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("pi2"), stateUp = stateUp)
        assertIndents(stateUp, "pi2", firstLineHeadIndent = 1, headIndent = 4)
    }

    @Test
    fun `interpretBlockClasses sets indents for pi3 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("pi3"), stateUp = stateUp)
        assertIndents(stateUp, "pi3", firstLineHeadIndent = 1, headIndent = 6)
    }

    @Test
    fun `interpretBlockClasses sets head indent for li1, ili, and ili1 classes`() {
        listOf("li1", "ili", "ili1").forEach { className ->
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 2)
        }
    }

    @Test
    fun `interpretBlockClasses sets head indent for li2 and ili2 classes`() {
        listOf("li2", "ili2").forEach { className ->
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 4)
        }
    }

    @Test
    fun `interpretBlockClasses sets head indent for li3 and ili3 classes`() {
        listOf("li3", "ili3").forEach { className ->
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 6)
        }
    }

    @Test
    fun `interpretBlockClasses sets head indent for li4 and ili4 classes`() {
        listOf("li4", "ili4").forEach { className ->
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 8)
        }
    }

    @Test
    fun `interpretBlockClasses sets first level indent for q, q1, iq, and iq1 classes`() {
        listOf("q", "q1", "iq", "iq1").forEach { className ->
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 2)
        }
    }

    @Test
    fun `interpretBlockClasses sets head indent for q2 and iq2 classes`() {
        listOf("q2", "iq2").forEach { className ->
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 4)
        }
    }

    @Test
    fun `interpretBlockClasses sets head indent for q3 and iq3 classes`() {
        listOf("q3", "iq3").forEach { className ->
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 6)
        }
    }

    @Test
    fun `interpretBlockClasses sets head indent for q4 and iq4 classes`() {
        listOf("q4", "iq4").forEach { className ->
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 8)
        }
    }

    @Test
    fun `interpretBlockClasses sets margins and increasing head indents for qm classes`() {
        val expectedHeadIndents =
            mapOf("qm" to 0, "qm1" to 2, "qm2" to 4, "qm3" to 6, "qm4" to 8)
        expectedHeadIndents.forEach { (className, headIndent) ->
            val stateDown = defaultStateDown()
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateDown = stateDown, stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = headIndent)
            assertEquals((0.50f * fontSize).dp, stateDown.marginTop)
            assertEquals((0.50f * fontSize).dp, stateDown.marginBottom)
        }
    }

    @Test
    fun `interpretBlockClasses sets indent and margins for pm, pmc, and pmo classes`() {
        listOf("pm", "pmc", "pmo").forEach { className ->
            val stateDown = defaultStateDown()
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateDown = stateDown, stateUp = stateUp)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 2)
            assertEquals((0.50f * fontSize).dp, stateDown.marginTop)
            assertEquals((0.50f * fontSize).dp, stateDown.marginBottom)
        }
    }

    @Test
    fun `interpretBlockClasses sets Center, medium font, and margins for cl class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("cl"), stateDown = stateDown)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(BibleTextFontOption.FONT_117EM_500, stateDown.currentFont)
        assertEquals(0.dp, stateDown.marginTop)
        assertEquals((0.25f * fontSize).dp, stateDown.marginBottom)
    }

    @Test
    fun `interpretBlockClasses sets italic HEADER styling for d class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("d"), stateDown = stateDown)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(BibleTextFontOption.FONT_100EM_ITALIC, stateDown.currentFont)
        assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
        assertEquals((0.60f * fontSize).dp, stateDown.marginTop)
        assertEquals((1.20f * fontSize).dp, stateDown.marginBottom)
    }

    @Test
    fun `interpretBlockClasses sets BOOK_TITLE styling for imt class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("imt"), stateDown = stateDown)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(BibleTextFontOption.FONT_117EM_500, stateDown.currentFont)
        assertEquals(BibleTextCategory.BOOK_TITLE, stateDown.textCategory)
    }

    @Test
    fun `interpretBlockClasses sets medium centered styling for is, is1, and is2 classes`() {
        mapOf(
            "is" to (fontSize / 2).dp,
            "is1" to (fontSize / 2).dp,
            "is2" to (fontSize / 3).dp,
        ).forEach { (className, expectedMarginTop) ->
            val stateDown = defaultStateDown()
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateDown = stateDown, stateUp = stateUp)
            assertEquals(BibleTextFontOption.FONT_100EM_500, stateDown.currentFont)
            assertEquals(TextAlign.Center, stateDown.alignment)
            assertEquals(expectedMarginTop, stateDown.marginTop)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 0)
        }
    }

    @Test
    fun `interpretBlockClasses sets centered italic styling for mr class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("mr"), stateDown = stateDown)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(BibleTextFontOption.FONT_117EM_500_ITALIC, stateDown.currentFont)
        assertEquals(0.dp, stateDown.marginTop)
        assertEquals((0.60f * fontSize).dp, stateDown.marginBottom)
    }

    @Test
    fun `interpretBlockClasses sets centered medium styling for ms class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("ms"), stateDown = stateDown)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(BibleTextFontOption.FONT_100EM_500, stateDown.currentFont)
        assertEquals(0.dp, stateDown.marginTop)
        assertEquals((0.60f * fontSize).dp, stateDown.marginBottom)
    }

    @Test
    fun `interpretBlockClasses sets larger centered styling for ms1 class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("ms1"), stateDown = stateDown)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals(BibleTextFontOption.FONT_117EM_500, stateDown.currentFont)
        assertEquals((0.50f * fontSize).dp, stateDown.marginTop)
        assertEquals((0.50f * fontSize).dp, stateDown.marginBottom)
    }

    @Test
    fun `interpretBlockClasses sets centered medium styling for ms2, ms3, and ms4 classes`() {
        listOf("ms2", "ms3", "ms4").forEach { className ->
            val stateDown = defaultStateDown()
            callInterpretBlock(listOf(className), stateDown = stateDown)
            assertEquals(TextAlign.Center, stateDown.alignment)
            assertEquals(BibleTextFontOption.FONT_100EM_500, stateDown.currentFont)
            assertEquals((0.50f * fontSize).dp, stateDown.marginTop)
            assertEquals((0.50f * fontSize).dp, stateDown.marginBottom)
        }
    }

    @Test
    fun `interpretBlockClasses sets italic HEADER styling for qa class`() {
        val stateDown = defaultStateDown()
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("qa"), stateDown = stateDown, stateUp = stateUp)
        assertEquals(BibleTextFontOption.FONT_117EM_500_ITALIC, stateDown.currentFont)
        assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
        assertEquals((0.50f * fontSize).dp, stateDown.marginTop)
        assertEquals((0.50f * fontSize).dp, stateDown.marginBottom)
        assertEquals(0, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets italic styling for sp class`() {
        val stateDown = defaultStateDown()
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("sp"), stateDown = stateDown, stateUp = stateUp)
        assertEquals(BibleTextFontOption.FONT_117EM_500_ITALIC, stateDown.currentFont)
        assertEquals((0.50f * fontSize).dp, stateDown.marginTop)
        assertEquals((0.50f * fontSize).dp, stateDown.marginBottom)
        assertIndents(stateUp, "sp", firstLineHeadIndent = 0, headIndent = 0)
    }

    @Test
    fun `interpretBlockClasses sets medium font and margin bottom for s1 class`() {
        val stateDown = defaultStateDown()
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("s1"), stateDown = stateDown, stateUp = stateUp)
        assertEquals(BibleTextFontOption.FONT_117EM_500, stateDown.currentFont)
        assertEquals(0.dp, stateDown.marginTop)
        assertEquals((0.25f * fontSize).dp, stateDown.marginBottom)
        assertEquals(0, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets medium italic font for s2, s3, and s4 classes`() {
        listOf("s2", "s3", "s4").forEach { className ->
            val stateDown = defaultStateDown()
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateDown = stateDown, stateUp = stateUp)
            assertEquals(BibleTextFontOption.FONT_100EM_500_ITALIC, stateDown.currentFont)
            assertEquals((0.5f * fontSize).dp, stateDown.marginTop)
            assertEquals((0.5f * fontSize).dp, stateDown.marginBottom)
            assertEquals(0, stateUp.headIndent)
        }
    }

    @Test
    fun `interpretBlockClasses sets medium centered styling for iot class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("iot"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.FONT_100EM_500, stateDown.currentFont)
        assertEquals(TextAlign.Center, stateDown.alignment)
        assertEquals((fontSize / 3).dp, stateDown.marginTop)
    }

    @Test
    fun `interpretBlockClasses sets head indent for io and io1 classes`() {
        listOf("io", "io1").forEach { className ->
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateUp = stateUp)
            assertEquals(2, stateUp.headIndent)
        }
    }

    @Test
    fun `interpretBlockClasses sets head indent for io2 class`() {
        val stateUp = defaultStateUp()
        callInterpretBlock(listOf("io2"), stateUp = stateUp)
        assertEquals(3, stateUp.headIndent)
    }

    @Test
    fun `interpretBlockClasses sets head indent for io3 and io4 classes`() {
        listOf("io3", "io4").forEach { className ->
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateUp = stateUp)
            assertEquals(4, stateUp.headIndent)
        }
    }

    @Test
    fun `interpretBlockClasses sets BOOK_TITLE styling for imt1, imte, and imte1 classes`() {
        listOf("imt1", "imte", "imte1").forEach { className ->
            val stateDown = defaultStateDown()
            callInterpretBlock(listOf(className), stateDown = stateDown)
            assertEquals(BibleTextCategory.BOOK_TITLE, stateDown.textCategory)
            assertEquals(BibleTextFontOption.FONT_100EM_500, stateDown.currentFont)
            assertEquals(TextAlign.Center, stateDown.alignment)
        }
    }

    @Test
    fun `interpretBlockClasses sets italic BOOK_TITLE styling for imt2 and imte2 classes`() {
        listOf("imt2", "imte2").forEach { className ->
            val stateDown = defaultStateDown()
            callInterpretBlock(listOf(className), stateDown = stateDown)
            assertEquals(BibleTextCategory.BOOK_TITLE, stateDown.textCategory)
            assertEquals(BibleTextFontOption.FONT_100EM_ITALIC, stateDown.currentFont)
            assertEquals(TextAlign.Center, stateDown.alignment)
            assertEquals((fontSize / 2).dp, stateDown.marginTop)
        }
    }

    @Test
    fun `interpretBlockClasses sets BOOK_TITLE styling for imt3 and imt4 classes`() {
        listOf("imt3", "imt4").forEach { className ->
            val stateDown = defaultStateDown()
            callInterpretBlock(listOf(className), stateDown = stateDown)
            assertEquals(BibleTextCategory.BOOK_TITLE, stateDown.textCategory)
            assertEquals(BibleTextFontOption.FONT_100EM_500, stateDown.currentFont)
            assertEquals(TextAlign.Center, stateDown.alignment)
            assertEquals((fontSize / 3).dp, stateDown.marginTop)
        }
    }

    @Test
    fun `interpretBlockClasses sets smaller italic font for r class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("r"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.FONT_076EM_ITALIC, stateDown.currentFont)
        assertEquals(0.dp, stateDown.marginTop)
    }

    @Test
    fun `interpretBlockClasses sets italic font for sr class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("sr"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.FONT_100EM_ITALIC, stateDown.currentFont)
    }

    @Test
    fun `interpretBlockClasses does not change state for no-op classes`() {
        listOf("b", "lh", "li", "lf", "po", "ior").forEach { className ->
            val stateDown = defaultStateDown()
            val stateUp = defaultStateUp()
            callInterpretBlock(listOf(className), stateDown = stateDown, stateUp = stateUp)
            assertEquals(BibleTextFontOption.FONT_100EM, stateDown.currentFont)
            assertEquals(BibleTextCategory.SCRIPTURE, stateDown.textCategory)
            assertEquals(TextAlign.Start, stateDown.alignment)
            assertEquals(0.dp, stateDown.marginTop)
            assertEquals(0.dp, stateDown.marginBottom)
            assertIndents(stateUp, className, firstLineHeadIndent = 0, headIndent = 0)
        }
    }

    @Test
    fun `interpretBlockClasses does not change state for truly unknown class`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("zzz"), stateDown = stateDown)
        assertEquals(BibleTextFontOption.FONT_100EM, stateDown.currentFont)
        assertEquals(BibleTextCategory.SCRIPTURE, stateDown.textCategory)
        assertEquals(TextAlign.Start, stateDown.alignment)
    }

    @Test
    fun `interpretBlockClasses takes header font from sibling class of yv-h`() {
        mapOf(
            "s1" to BibleTextFontOption.FONT_117EM_500,
            "s2" to BibleTextFontOption.FONT_100EM_500_ITALIC,
            "mr" to BibleTextFontOption.FONT_117EM_500_ITALIC,
            "ms" to BibleTextFontOption.FONT_100EM_500,
        ).forEach { (className, expectedFont) ->
            val stateDown = defaultStateDown()
            callInterpretBlock(listOf("yv-h", className), stateDown = stateDown)
            assertEquals(
                expectedFont,
                stateDown.currentFont,
                "Expected font $expectedFont for header sibling class '$className'",
            )
            assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
        }
    }

    @Test
    fun `interpretBlockClasses sets BOOK_TITLE category for yv-h with imt sibling`() {
        val stateDown = defaultStateDown()
        callInterpretBlock(listOf("yv-h", "imt"), stateDown = stateDown)
        assertEquals(BibleTextCategory.BOOK_TITLE, stateDown.textCategory)
    }

    @Test
    fun `interpretBlockClasses disables rendering for yv-h when renderHeadlines is false`() {
        listOf("yv-h", "yvh").forEach { headerClass ->
            val stateUp = defaultStateUp()
            callInterpretBlock(
                listOf(headerClass, "s1"),
                stateIn = defaultStateIn(renderHeadlines = false),
                stateUp = stateUp,
            )
            assertFalse(stateUp.rendering)
            assertEquals(0, stateUp.firstLineHeadIndent)
        }
    }

    @Test
    fun `interpretBlockClasses keeps rendering for yv-h and yvh when renderHeadlines is true`() {
        listOf("yv-h", "yvh").forEach { headerClass ->
            val stateDown = defaultStateDown()
            val stateUp = defaultStateUp()
            callInterpretBlock(
                listOf(headerClass, "s1"),
                stateIn = defaultStateIn(renderHeadlines = true),
                stateDown = stateDown,
                stateUp = stateUp,
            )
            assertEquals(BibleTextCategory.HEADER, stateDown.textCategory)
            assertTrue(stateUp.rendering)
        }
    }
}
