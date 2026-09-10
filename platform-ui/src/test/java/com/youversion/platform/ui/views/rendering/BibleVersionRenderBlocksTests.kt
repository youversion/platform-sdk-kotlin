package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BibleVersionRenderBlocksTests {
    // ----- indentation

    private suspend fun renderSingleBlock(
        className: String,
        text: String,
    ): BibleTextBlock {
        val html =
            """
            <div>
                <div class="$className">
                    <span class="yv-v" v="1"></span>
                    $text
                </div>
            </div>
            """.trimIndent()

        val blocks = renderBlocks(html, FULL_CHAPTER_REF)
        return blocks.first { it.text.text.contains(text) }
    }

    private fun assertIndents(
        block: BibleTextBlock,
        className: String,
        firstLineHeadIndent: Int,
        headIndent: Int,
    ) {
        assertEquals(
            firstLineHeadIndent,
            block.firstLineHeadIndent,
            "Expected first line head indent $firstLineHeadIndent for class '$className'",
        )
        assertEquals(
            headIndent,
            block.headIndent,
            "Expected head indent $headIndent for class '$className'",
        )
    }

    @Test
    fun `p and related classes produce first line indent`() =
        runTest {
            listOf("p", "ip", "imi").forEach { className ->
                val block = renderSingleBlock(className, "Indented paragraph.")
                assertIndents(block, className, firstLineHeadIndent = 1, headIndent = 0)
            }
        }

    @Test
    fun `mi class produces head indent`() =
        runTest {
            val block = renderSingleBlock("mi", "Margin indent text.")
            assertIndents(block, "mi", firstLineHeadIndent = 0, headIndent = 2)
        }

    @Test
    fun `pi class produces no indent`() =
        runTest {
            val block = renderSingleBlock("pi", "Paragraph indent text.")
            assertIndents(block, "pi", firstLineHeadIndent = 0, headIndent = 0)
        }

    @Test
    fun `pi1 and ipi classes produce first line and head indent`() =
        runTest {
            listOf("pi1", "ipi").forEach { className ->
                val block = renderSingleBlock(className, "Paragraph indent text.")
                assertIndents(block, className, firstLineHeadIndent = 1, headIndent = 2)
            }
        }

    @Test
    fun `pi2 class produces first line and head indent`() =
        runTest {
            val block = renderSingleBlock("pi2", "Deep indent text.")
            assertIndents(block, "pi2", firstLineHeadIndent = 1, headIndent = 4)
        }

    @Test
    fun `pi3 class produces first line and deeper head indent`() =
        runTest {
            val block = renderSingleBlock("pi3", "Deepest indent text.")
            assertIndents(block, "pi3", firstLineHeadIndent = 1, headIndent = 6)
        }

    @Test
    fun `li1 and aliases produce head indent`() =
        runTest {
            listOf("li1", "ili", "ili1").forEach { className ->
                val block = renderSingleBlock(className, "List item text.")
                assertIndents(block, className, firstLineHeadIndent = 0, headIndent = 2)
            }
        }

    @Test
    fun `li2 and ili2 classes produce deeper head indent than li1`() =
        runTest {
            listOf("li2", "ili2").forEach { className ->
                val block = renderSingleBlock(className, "List level 2 text.")
                assertIndents(block, className, firstLineHeadIndent = 0, headIndent = 4)
            }
        }

    @Test
    fun `li3 and ili3 classes produce head indent`() =
        runTest {
            listOf("li3", "ili3").forEach { className ->
                val block = renderSingleBlock(className, "List level 3 text.")
                assertIndents(block, className, firstLineHeadIndent = 0, headIndent = 6)
            }
        }

    @Test
    fun `li4 and ili4 classes produce head indent`() =
        runTest {
            listOf("li4", "ili4").forEach { className ->
                val block = renderSingleBlock(className, "List level 4 text.")
                assertIndents(block, className, firstLineHeadIndent = 0, headIndent = 8)
            }
        }

    @Test
    fun `default poetry level uses first level indent`() =
        runTest {
            listOf("q", "q1", "iq", "iq1", "qm1").forEach { className ->
                val block = renderSingleBlock(className, "Poetry line.")
                assertIndents(block, className, firstLineHeadIndent = 0, headIndent = 2)
            }
        }

    @Test
    fun `qm class produces no indent`() =
        runTest {
            val block = renderSingleBlock("qm", "Poetry line.")
            assertIndents(block, "qm", firstLineHeadIndent = 0, headIndent = 0)
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

    // ----- margins

    @Test
    fun `s1 header blocks have margin bottom and no margin top`() =
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
            assertEquals(0.dp, headerBlock.marginTop)
            assertTrue(headerBlock.marginBottom.value > 0)
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
    fun `iot class applies medium font weight`() =
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
            val mediumStyles =
                block.text.spanStyles.filter { it.start <= titleStart && it.end > titleStart }
            assertTrue(mediumStyles.any { it.item.fontWeight == FontWeight.Medium })
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
            assertTrue(verseStyles.any { it.item.baselineShift == RENDERING_TEST_FONTS.verseNumBaselineShift })
            assertTrue(
                verseStyles.any {
                    it.item.color.alpha < 1f && it.item.color.alpha > 0f
                },
            )
        }
}
