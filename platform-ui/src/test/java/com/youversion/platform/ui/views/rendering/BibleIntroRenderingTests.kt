package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.youversion.platform.ui.views.BibleTextFonts
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BibleIntroRenderingTests {
    private val fonts = BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 16.sp)

    private suspend fun renderIntroBlocks(
        html: String,
        footnoteMode: BibleTextFootnoteMode = BibleTextFootnoteMode.IMAGE,
        renderHeadlines: Boolean = true,
    ): List<BibleTextBlock> {
        val blocks =
            BibleVersionRendering.introTextBlocks(
                htmlContent = html,
                versionId = 111,
                bookUSFM = "GEN",
                renderHeadlines = renderHeadlines,
                footnoteMode = footnoteMode,
                textColor = Color.Black,
                fonts = fonts,
            )
        assertNotNull(blocks)
        return blocks
    }

    private fun List<BibleTextBlock>.hasHeaderContaining(text: String): Boolean =
        any { block ->
            val annotations =
                block.text.getStringAnnotations(
                    tag = BibleTextCategoryAttribute.NAME,
                    start = 0,
                    end = block.text.length,
                )
            annotations.any { it.item == BibleTextCategory.HEADER.name } &&
                block.text.text.contains(text)
        }

    private fun List<BibleTextBlock>.hasFootnoteAnnotation(): Boolean =
        any { block ->
            val annotations =
                block.text.getStringAnnotations(
                    tag = BibleTextCategoryAttribute.NAME,
                    start = 0,
                    end = block.text.length,
                )
            annotations.any {
                it.item == BibleTextCategory.FOOTNOTE_IMAGE.name ||
                    it.item == BibleTextCategory.FOOTNOTE_MARKER.name
            }
        }

    @Test
    fun `intro blocks with footnote in IMAGE mode include footnote annotation`() =
        runTest {
            val html = INTRO_HTML_WITH_FOOTNOTE

            val blocks = renderIntroBlocks(html, footnoteMode = BibleTextFootnoteMode.IMAGE)
            assertTrue(blocks.hasFootnoteAnnotation())
        }

    @Test
    fun `intro blocks with footnote in IMAGE mode populate footnotes list`() =
        runTest {
            val html = INTRO_HTML_WITH_FOOTNOTE

            val blocks = renderIntroBlocks(html, footnoteMode = BibleTextFootnoteMode.IMAGE)
            val allFootnotes = blocks.flatMap { it.footnotes }
            assertTrue(allFootnotes.isNotEmpty())
            assertTrue(allFootnotes.any { it.text.contains("footnote text") })
        }

    @Test
    fun `intro blocks with footnote in NONE mode do not include footnote annotation`() =
        runTest {
            val html = INTRO_HTML_WITH_FOOTNOTE

            val blocks = renderIntroBlocks(html, footnoteMode = BibleTextFootnoteMode.NONE)
            assertTrue(!blocks.hasFootnoteAnnotation())
        }

    @Test
    fun `intro blocks with footnote in NONE mode have empty footnotes list`() =
        runTest {
            val html = INTRO_HTML_WITH_FOOTNOTE

            val blocks = renderIntroBlocks(html, footnoteMode = BibleTextFootnoteMode.NONE)
            val allFootnotes = blocks.flatMap { it.footnotes }
            assertEquals(0, allFootnotes.size)
        }

    @Test
    fun `intro blocks with footnote in LETTERS mode include footnote marker annotation`() =
        runTest {
            val html = INTRO_HTML_WITH_FOOTNOTE

            val blocks = renderIntroBlocks(html, footnoteMode = BibleTextFootnoteMode.LETTERS)
            assertTrue(blocks.hasFootnoteAnnotation())
            val allFootnotes = blocks.flatMap { it.footnotes }
            assertTrue(allFootnotes.isNotEmpty())
        }

    @Test
    fun `intro blocks render scripture text correctly`() =
        runTest {
            val html = INTRO_HTML_WITH_FOOTNOTE

            val blocks = renderIntroBlocks(html)
            assertTrue(blocks.any { it.text.text.contains("Some intro text") })
        }

    @Test
    fun `intro blocks render headers`() =
        runTest {
            val html = INTRO_HTML_WITH_HEADER

            val blocks = renderIntroBlocks(html)
            assertTrue(blocks.hasHeaderContaining("Introduction"))
            assertTrue(blocks.any { it.text.text.contains("Intro paragraph text.") })
        }

    @Test
    fun `intro blocks render tables with rows`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <table>
                            <tr><td>Col1</td><td>Col2</td></tr>
                        </table>
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderIntroBlocks(html)
            val tableBlock = blocks.firstOrNull { it.rows.isNotEmpty() }
            assertNotNull(tableBlock)
            assertEquals(1, tableBlock.rows.size)
            assertEquals(2, tableBlock.rows.first().size)
            assertTrue(
                tableBlock.rows
                    .first()[0]
                    .text
                    .contains("Col1"),
            )
            assertTrue(
                tableBlock.rows
                    .first()[1]
                    .text
                    .contains("Col2"),
            )
        }

    @Test
    fun `intro blocks return empty list for empty HTML`() =
        runTest {
            val result =
                BibleVersionRendering.introTextBlocks(
                    htmlContent = "<div></div>",
                    versionId = 111,
                    bookUSFM = "GEN",
                    footnoteMode = BibleTextFootnoteMode.IMAGE,
                    textColor = Color.Black,
                    fonts = fonts,
                )
            assertNotNull(result)
            assertTrue(result.isEmpty())
        }

    @Test
    fun `intro blocks with renderHeadlines false do not render headers`() =
        runTest {
            val html = INTRO_HTML_WITH_HEADER

            val blocks = renderIntroBlocks(html, renderHeadlines = false)
            assertFalse(blocks.hasHeaderContaining("Introduction"))
            assertTrue(blocks.any { it.text.text.contains("Intro paragraph text.") })
        }

    @Test
    fun `intro blocks return null for malformed HTML`() =
        runTest {
            val result =
                BibleVersionRendering.introTextBlocks(
                    htmlContent = "<div><span>unclosed",
                    versionId = 111,
                    bookUSFM = "GEN",
                    footnoteMode = BibleTextFootnoteMode.IMAGE,
                    textColor = Color.Black,
                    fonts = fonts,
                )
            assertNull(result)
        }

    @Test
    fun `intro blocks with qr class produce end alignment`() =
        runTest {
            val html =
                """
                <div>
                    <div class="qr">
                        Right aligned intro text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderIntroBlocks(html)
            val block = blocks.first { it.text.text.contains("Right aligned intro text.") }
            assertEquals(TextAlign.End, block.alignment)
        }

    @Test
    fun `intro blocks with qc class produce center alignment`() =
        runTest {
            val html =
                """
                <div>
                    <div class="qc">
                        Centered intro text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderIntroBlocks(html)
            val block = blocks.first { it.text.text.contains("Centered intro text.") }
            assertEquals(TextAlign.Center, block.alignment)
        }

    @Test
    fun `intro blocks with p class produce first line indent`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        Indented intro paragraph.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderIntroBlocks(html)
            val block = blocks.first { it.text.text.contains("Indented intro paragraph.") }
            val paragraphStyle =
                block.text.paragraphStyles
                    .first()
                    .item
            assertTrue(paragraphStyle.textIndent!!.firstLine.value > 0)
        }
}

private val INTRO_HTML_WITH_HEADER =
    """
    <div>
        <div class="yv-h s1"><span>Introduction</span></div>
        <div class="p">Intro paragraph text.</div>
    </div>
    """.trimIndent()

private val INTRO_HTML_WITH_FOOTNOTE =
    """
    <div>
        <div class="p">
            <span class="yv-v" v="1"></span>
            Some intro text with a
            <span class="yv-n f"><span class="fr">1:1</span><span class="ft">footnote text</span></span>
            marker here.
        </div>
    </div>
    """.trimIndent()
