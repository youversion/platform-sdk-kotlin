package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BibleVersionRenderingStyleTests {
    private val oneIndent = RENDERING_TEST_FONTS.baseSize.value
    private val twoIndent = oneIndent * 2
    private val threeIndent = oneIndent * 3
    private val fourIndent = oneIndent * 4

    // ----- indentation

    @Test
    fun `p and related classes produce first line indent`() =
        runTest {
            listOf("p", "ip", "imi", "ipi").forEach { className ->
                val html =
                    """
                    <div>
                        <div class="$className">
                            <span class="yv-v" v="1"></span>
                            Indented paragraph.
                        </div>
                    </div>
                    """.trimIndent()

                val blocks = renderBlocks(html, FULL_CHAPTER_REF)
                val block = blocks.first { it.text.text.contains("Indented paragraph.") }
                val paragraphStyle =
                    block.text.paragraphStyles
                        .first()
                        .item
                assertEquals(
                    twoIndent,
                    paragraphStyle.textIndent!!.firstLine.value,
                    "Expected two-indent first line for class '$className'",
                )
            }
        }

    @Test
    fun `mi class produces head indent`() =
        runTest {
            val html =
                """
                <div>
                    <div class="mi">
                        <span class="yv-v" v="1"></span>
                        Margin indent text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val block = blocks.first { it.text.text.contains("Margin indent text.") }
            val paragraphStyle =
                block.text.paragraphStyles
                    .first()
                    .item
            assertEquals(twoIndent, paragraphStyle.textIndent!!.restLine.value)
        }

    @Test
    fun `pi and pi1 classes produce first line indent`() =
        runTest {
            listOf("pi", "pi1").forEach { className ->
                val html =
                    """
                    <div>
                        <div class="$className">
                            <span class="yv-v" v="1"></span>
                            Paragraph indent text.
                        </div>
                    </div>
                    """.trimIndent()

                val blocks = renderBlocks(html, FULL_CHAPTER_REF)
                val block = blocks.first { it.text.text.contains("Paragraph indent text.") }
                val paragraphStyle =
                    block.text.paragraphStyles
                        .first()
                        .item
                assertEquals(
                    oneIndent,
                    paragraphStyle.textIndent!!.firstLine.value,
                    "Expected one-indent first line for class '$className'",
                )
            }
        }

    @Test
    fun `pi2 class produces first line and head indent`() =
        runTest {
            val html =
                """
                <div>
                    <div class="pi2">
                        <span class="yv-v" v="1"></span>
                        Deep indent text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val block = blocks.first { it.text.text.contains("Deep indent text.") }
            val paragraphStyle =
                block.text.paragraphStyles
                    .first()
                    .item
            assertEquals(threeIndent, paragraphStyle.textIndent!!.firstLine.value)
            assertEquals(twoIndent, paragraphStyle.textIndent!!.restLine.value)
        }

    @Test
    fun `pi3 class produces first line and deeper head indent`() =
        runTest {
            val html =
                """
                <div>
                    <div class="pi3">
                        <span class="yv-v" v="1"></span>
                        Deepest indent text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val block = blocks.first { it.text.text.contains("Deepest indent text.") }
            val paragraphStyle =
                block.text.paragraphStyles
                    .first()
                    .item
            assertEquals(fourIndent, paragraphStyle.textIndent!!.firstLine.value)
            assertEquals(threeIndent, paragraphStyle.textIndent!!.restLine.value)
        }

    @Test
    fun `li1 and aliases produce head indent`() =
        runTest {
            listOf("li1", "ili", "ili1").forEach { className ->
                val html =
                    """
                    <div>
                        <div class="$className">
                            <span class="yv-v" v="1"></span>
                            List item text.
                        </div>
                    </div>
                    """.trimIndent()

                val blocks = renderBlocks(html, FULL_CHAPTER_REF)
                val block = blocks.first { it.text.text.contains("List item text.") }
                val paragraphStyle =
                    block.text.paragraphStyles
                        .first()
                        .item
                assertEquals(
                    oneIndent,
                    paragraphStyle.textIndent!!.restLine.value,
                    "Expected one-indent head for class '$className'",
                )
            }
        }

    @Test
    fun `li2 and ili2 classes produce deeper head indent than li1`() =
        runTest {
            listOf("li2", "ili2").forEach { className ->
                val html =
                    """
                    <div>
                        <div class="$className">
                            <span class="yv-v" v="1"></span>
                            List level 2 text.
                        </div>
                    </div>
                    """.trimIndent()

                val blocks = renderBlocks(html, FULL_CHAPTER_REF)
                val block = blocks.first { it.text.text.contains("List level 2 text.") }
                val paragraphStyle =
                    block.text.paragraphStyles
                        .first()
                        .item
                assertEquals(
                    twoIndent,
                    paragraphStyle.textIndent!!.restLine.value,
                    "Expected two-indent head for class '$className'",
                )
            }
        }

    @Test
    fun `li3 and ili3 classes produce head indent`() =
        runTest {
            listOf("li3", "ili3").forEach { className ->
                val html =
                    """
                    <div>
                        <div class="$className">
                            <span class="yv-v" v="1"></span>
                            List level 3 text.
                        </div>
                    </div>
                    """.trimIndent()

                val blocks = renderBlocks(html, FULL_CHAPTER_REF)
                val block = blocks.first { it.text.text.contains("List level 3 text.") }
                val paragraphStyle =
                    block.text.paragraphStyles
                        .first()
                        .item
                assertEquals(
                    threeIndent,
                    paragraphStyle.textIndent!!.restLine.value,
                    "Expected three-indent head for class '$className'",
                )
            }
        }

    @Test
    fun `li4 and ili4 classes produce head indent`() =
        runTest {
            listOf("li4", "ili4").forEach { className ->
                val html =
                    """
                    <div>
                        <div class="$className">
                            <span class="yv-v" v="1"></span>
                            List level 4 text.
                        </div>
                    </div>
                    """.trimIndent()

                val blocks = renderBlocks(html, FULL_CHAPTER_REF)
                val block = blocks.first { it.text.text.contains("List level 4 text.") }
                val paragraphStyle =
                    block.text.paragraphStyles
                        .first()
                        .item
                assertEquals(
                    fourIndent,
                    paragraphStyle.textIndent!!.restLine.value,
                    "Expected four-indent head for class '$className'",
                )
            }
        }

    @Test
    fun `q1 and aliases produce no indent`() =
        runTest {
            listOf("q", "q1", "iq", "iq1", "qm", "qm1").forEach { className ->
                val html =
                    """
                    <div>
                        <div class="$className">
                            <span class="yv-v" v="1"></span>
                            Poetry line.
                        </div>
                    </div>
                    """.trimIndent()

                val blocks = renderBlocks(html, FULL_CHAPTER_REF)
                val block = blocks.first { it.text.text.contains("Poetry line.") }
                val paragraphStyle =
                    block.text.paragraphStyles
                        .first()
                        .item
                assertEquals(
                    0f,
                    paragraphStyle.textIndent!!.firstLine.value,
                    "Expected no first line indent for class '$className'",
                )
                assertEquals(
                    0f,
                    paragraphStyle.textIndent!!.restLine.value,
                    "Expected no rest line indent for class '$className'",
                )
            }
        }

    // ----- alignment

    @Test
    fun `qr and pr classes produce end alignment`() =
        runTest {
            listOf("qr", "pr").forEach { className ->
                val html =
                    """
                    <div>
                        <div class="$className">
                            <span class="yv-v" v="1"></span>
                            Right aligned text.
                        </div>
                    </div>
                    """.trimIndent()

                val blocks = renderBlocks(html, FULL_CHAPTER_REF)
                val block = blocks.first { it.text.text.contains("Right aligned text.") }
                assertEquals(
                    TextAlign.End,
                    block.alignment,
                    "Expected end alignment for class '$className'",
                )
            }
        }

    @Test
    fun `pc and qc classes produce center alignment`() =
        runTest {
            listOf("pc", "qc").forEach { className ->
                val html =
                    """
                    <div>
                        <div class="$className">
                            <span class="yv-v" v="1"></span>
                            Centered text.
                        </div>
                    </div>
                    """.trimIndent()

                val blocks = renderBlocks(html, FULL_CHAPTER_REF)
                val block = blocks.first { it.text.text.contains("Centered text.") }
                assertEquals(
                    TextAlign.Center,
                    block.alignment,
                    "Expected center alignment for class '$className'",
                )
            }
        }

    // ----- margin top

    @Test
    fun `header blocks have margin top`() =
        runTest {
            val html =
                """
                <div>
                    <div class="yv-h s1"><span>Section Title</span></div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Verse text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val headerBlock = blocks.first { it.text.text.contains("Section Title") }
            assertTrue(headerBlock.marginTop.value > 0)
        }

    // ----- font styles

    @Test
    fun `small caps sc class applies font feature settings`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        The <span class="sc">Lord</span> spoke.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val block = blocks.first { it.text.text.contains("Lord") }
            val lordStart = block.text.text.indexOf("Lord")
            val scStyles =
                block.text.spanStyles.filter { it.start <= lordStart && it.end > lordStart }
            assertTrue(
                scStyles.any { it.item.fontFeatureSettings?.contains("smcp") == true },
            )
        }

    @Test
    fun `italic it class applies italic font style`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Normal <span class="it">italic text</span> end.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val block = blocks.first { it.text.text.contains("italic text") }
            val italicStart = block.text.text.indexOf("italic text")
            val italicStyles =
                block.text.spanStyles.filter { it.start <= italicStart && it.end > italicStart }
            assertTrue(
                italicStyles.any {
                    it.item.fontStyle == FontStyle.Italic
                },
            )
        }

    @Test
    fun `iot class applies bold font weight`() =
        runTest {
            val html =
                """
                <div>
                    <div class="iot">
                        <span class="yv-v" v="1"></span>
                        Outline Title
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val block = blocks.first { it.text.text.contains("Outline Title") }
            val titleStart = block.text.text.indexOf("Outline Title")
            val boldStyles =
                block.text.spanStyles.filter { it.start <= titleStart && it.end > titleStart }
            assertTrue(boldStyles.any { it.item.fontWeight == FontWeight.Bold })
        }

    // ----- words of Christ

    @Test
    fun `words of Christ with wj class apply wocColor`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Jesus said
                        <span class="wj">Truly I tell you</span>
                        end.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF, wocColor = Color.Red)
            val allText = blocks.joinToString("") { it.text.text }
            assertTrue(allText.contains("Truly I tell you"))

            val block = blocks.first { it.text.text.contains("Truly I tell you") }
            val wocStart = block.text.text.indexOf("Truly I tell you")
            val wocStyles =
                block.text.spanStyles.filter { it.start <= wocStart && it.end > wocStart }
            assertTrue(wocStyles.any { it.item.color == Color.Red })
        }

    // ----- verse number styling

    @Test
    fun `verse number has baseline shift and reduced opacity`() =
        runTest {
            val blocks =
                renderBlocks(SIMPLE_VERSE_HTML, FULL_CHAPTER_REF, renderVerseNumbers = true)
            val block = blocks.first()
            val verseNumText = "1\u00A0"
            val verseStart = block.text.text.indexOf(verseNumText)
            assertTrue(verseStart >= 0)

            val verseStyles =
                block.text.spanStyles.filter { it.start <= verseStart && it.end > verseStart }
            assertTrue(verseStyles.any { it.item.baselineShift == BaselineShift.Superscript })
            assertTrue(
                verseStyles.any {
                    it.item.color.alpha < 1f && it.item.color.alpha > 0f
                },
            )
        }
}
