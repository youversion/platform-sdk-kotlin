package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.bibles.domain.BibleReference
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    private fun BibleTextBlock.hasCategory(category: BibleTextCategory): Boolean =
        text
            .getStringAnnotations(BibleTextCategoryAttribute.NAME, 0, text.length)
            .any { it.item == category.name }

    @Test
    fun `p and related classes produce first line indent`() =
        runTest {
            listOf("p", "ip").forEach { className ->
                val block = renderSingleBlock(className, "Indented paragraph.")
                assertIndents(block, className, firstLineHeadIndent = 1, headIndent = 0)
            }
        }

    @Test
    fun `imi class produces head indent`() =
        runTest {
            val block = renderSingleBlock("imi", "Indented paragraph.")
            assertIndents(block, "imi", firstLineHeadIndent = 0, headIndent = 2)
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

    @Test
    fun `li and lim classes produce head indent`() =
        runTest {
            listOf("li", "lim").forEach { className ->
                val block = renderSingleBlock(className, "List item text.")
                assertIndents(block, className, firstLineHeadIndent = 0, headIndent = 2)
            }
        }

    @Test
    fun `lh class produces first line indent and margin top`() =
        runTest {
            val block = renderSingleBlock("lh", "List header text.")
            assertIndents(block, "lh", firstLineHeadIndent = 1, headIndent = 0)
            assertTrue(block.marginTop.value > 0)
        }

    @Test
    fun `po class produces first line indent and margins`() =
        runTest {
            val block = renderSingleBlock("po", "Letter opening.")
            assertIndents(block, "po", firstLineHeadIndent = 1, headIndent = 0)
            assertTrue(block.marginTop.value > 0)
            assertTrue(block.marginBottom.value > 0)
        }

    @Test
    fun `imq class produces head indent and italic font`() =
        runTest {
            val block = renderSingleBlock("imq", "Quoted intro text.")
            assertIndents(block, "imq", firstLineHeadIndent = 0, headIndent = 2)
            assertTrue(block.text.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        }

    @Test
    fun `ipq class produces first line and head indent and italic font`() =
        runTest {
            val block = renderSingleBlock("ipq", "Quoted intro paragraph.")
            assertIndents(block, "ipq", firstLineHeadIndent = 1, headIndent = 2)
            assertTrue(block.text.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
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

    @Test
    fun `cls class produces end alignment`() =
        runTest {
            val block = renderSingleBlock("cls", "Closing text.")
            assertEquals(TextAlign.End, block.alignment)
        }

    @Test
    fun `ipr class produces end alignment and italic font`() =
        runTest {
            val block = renderSingleBlock("ipr", "Right aligned intro text.")
            assertEquals(TextAlign.End, block.alignment)
            assertTrue(block.text.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
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

    @Test
    fun `s class produces medium font and margin bottom`() =
        runTest {
            val block = renderSingleBlock("s", "Section Title.")
            assertTrue(block.marginBottom.value > 0)
            assertIndents(block, "s", firstLineHeadIndent = 0, headIndent = 0)
            assertTrue(block.text.spanStyles.any { it.item.fontWeight == FontWeight.Medium })
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
    fun `k class applies bold font weight`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Normal <span class="k">keyword</span> end.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val block = blocks.first { it.text.text.contains("keyword") }
            val keywordStart = block.text.text.indexOf("keyword")
            val keywordStyles =
                block.text.spanStyles.filter { it.start <= keywordStart && it.end > keywordStart }
            assertTrue(keywordStyles.any { it.item.fontWeight == FontWeight.Bold })
        }

    @Test
    fun `sls and iqt classes apply italic font style`() =
        runTest {
            listOf("sls", "iqt").forEach { className ->
                val html =
                    """
                    <div>
                        <div class="p">
                            <span class="yv-v" v="1"></span>
                            Normal <span class="$className">styled text</span> end.
                        </div>
                    </div>
                    """.trimIndent()

                val blocks = renderBlocks(html, FULL_CHAPTER_REF)
                val block = blocks.first { it.text.text.contains("styled text") }
                val styledStart = block.text.text.indexOf("styled text")
                val styledStyles =
                    block.text.spanStyles.filter { it.start <= styledStart && it.end > styledStart }
                assertTrue(
                    styledStyles.any { it.item.fontStyle == FontStyle.Italic },
                    "Expected italic font style for class '$className'",
                )
            }
        }

    @Test
    fun `cd and qd classes apply italic font style`() =
        runTest {
            listOf("cd", "qd").forEach { className ->
                val block = renderSingleBlock(className, "Description text.")
                assertTrue(
                    block.text.spanStyles.any { it.item.fontStyle == FontStyle.Italic },
                    "Expected italic font style for class '$className'",
                )
                assertFalse(
                    block.text.spanStyles.any { it.item.fontWeight == FontWeight.Medium },
                    "Expected regular weight for class '$className'",
                )
            }
        }

    @Test
    fun `cd header applies medium italic font`() =
        runTest {
            val html =
                """
                <div>
                    <div class="yv-h cd"><span>Header description</span></div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Verse text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val block = blocks.first { it.text.text.contains("Header description") }
            assertTrue(
                block.text.spanStyles.any {
                    it.item.fontStyle == FontStyle.Italic && it.item.fontWeight == FontWeight.Medium
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
            assertTrue(verseStyles.any { it.item.baselineShift == RENDERING_TEST_FONTS.verseNumBaselineShift })
            assertTrue(
                verseStyles.any {
                    it.item.color.alpha < 1f && it.item.color.alpha > 0f
                },
            )
        }

    // ----- book titles

    @Test
    fun `mt1 class produces a centered book title`() =
        runTest {
            val block = renderSingleBlock("mt1", "Book Title.")
            assertEquals(TextAlign.Center, block.alignment)
            assertTrue(block.hasCategory(BibleTextCategory.BOOK_TITLE))
            assertTrue(block.text.spanStyles.any { it.item.fontWeight == FontWeight.Medium })
        }

    @Test
    fun `mt2 class produces a centered italic book title`() =
        runTest {
            val block = renderSingleBlock("mt2", "Book Subtitle.")
            assertEquals(TextAlign.Center, block.alignment)
            assertTrue(block.hasCategory(BibleTextCategory.BOOK_TITLE))
            assertTrue(block.text.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        }

    @Test
    fun `imt4 class produces a centered italic book title with small margins`() =
        runTest {
            val block = renderSingleBlock("imt4", "Intro Title.")
            val margin = (0.15f * RENDERING_TEST_FONTS.baseSize.value).dp
            assertEquals(TextAlign.Center, block.alignment)
            assertTrue(block.hasCategory(BibleTextCategory.BOOK_TITLE))
            assertTrue(block.text.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
            assertEquals(margin, block.marginTop)
            assertEquals(margin, block.marginBottom)
        }

    @Test
    fun `imte and imte1 classes produce a centered bold book title with margins`() =
        runTest {
            listOf("imte", "imte1").forEach { className ->
                val block = renderSingleBlock(className, "Intro Ending.")
                val fontSize = RENDERING_TEST_FONTS.baseSize.value
                assertEquals(TextAlign.Center, block.alignment, "Expected center alignment for class '$className'")
                assertTrue(
                    block.hasCategory(BibleTextCategory.BOOK_TITLE),
                    "Expected book title for class '$className'",
                )
                assertTrue(
                    block.text.spanStyles.any { it.item.fontWeight == FontWeight.Bold },
                    "Expected bold font weight for class '$className'",
                )
                assertEquals(fontSize.dp, block.marginTop, "Expected 1em margin top for class '$className'")
                assertEquals(
                    (0.25f * fontSize).dp,
                    block.marginBottom,
                    "Expected 0.25em margin bottom for class '$className'",
                )
            }
        }

    // ----- stanza breaks

    @Test
    fun `empty stanza breaks produce unlinked blank lines`() =
        runTest {
            val html =
                "<div><div class=\"b\"></div><div class=\"b\">\u00A0</div><div class=\"p\">Text.</div></div>"
            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            assertEquals(3, blocks.size)
            blocks.take(2).forEach { block ->
                assertEquals(NO_BREAK_SPACE.toString(), block.text.text)
                assertTrue(block.isVisible)
                assertTrue(block.isBlankLine)
                assertTrue(
                    block.text
                        .getStringAnnotations(BibleReferenceAttribute.NAME, 0, block.text.length)
                        .isEmpty(),
                )
            }
        }

    @Test
    fun `blank text is not visible but a non-breaking space is`() {
        fun block(text: String) =
            BibleTextBlock(
                text = AnnotatedString(text),
                chapter = 1,
                firstLineHeadIndent = 0,
                headIndent = 0,
                marginTop = 0.dp,
                marginBottom = 0.dp,
                alignment = TextAlign.Start,
                footnotes = emptyList(),
            )

        assertFalse(block("").isVisible)
        assertFalse(block(" \n ").isVisible)
        assertFalse(block(" \n ").isBlankLine)
        assertTrue(block(NO_BREAK_SPACE.toString()).isVisible)
        assertTrue(block(NO_BREAK_SPACE.toString()).isBlankLine)
        assertTrue(block("Text").isVisible)
        assertFalse(block("Text").isBlankLine)
    }

    @Test
    fun `stanza break preserves text and respects verse range`() =
        runTest {
            val html =
                "<div><div class=\"b\"></div><div class=\"b\"><span class=\"yv-v\" v=\"1\"></span>First verse.</div>" +
                    "<div class=\"b\"></div><div class=\"p\"><span class=\"yv-v\" v=\"2\"></span>Second verse.</div>" +
                    "<div class=\"b\"></div></div>"
            val chapterBlocks = renderBlocks(html, FULL_CHAPTER_REF)
            assertEquals(
                listOf(
                    NO_BREAK_SPACE.toString(),
                    "First verse.",
                    NO_BREAK_SPACE.toString(),
                    "Second verse.",
                    NO_BREAK_SPACE.toString(),
                ),
                chapterBlocks.map { it.text.text },
            )

            val selectedBlocks =
                renderBlocks(
                    html,
                    BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verseStart = 2, verseEnd = 2),
                )
            assertEquals(listOf("Second verse.", NO_BREAK_SPACE.toString()), selectedBlocks.map { it.text.text })
        }
}
