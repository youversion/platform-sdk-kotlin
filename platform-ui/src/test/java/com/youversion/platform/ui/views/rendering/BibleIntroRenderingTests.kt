package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.youversion.platform.ui.views.BibleTextFonts
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BibleIntroRenderingTests {
    private val fonts = BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 16.sp)

    private suspend fun renderIntroBlocks(
        html: String,
        footnoteMode: BibleTextFootnoteMode = BibleTextFootnoteMode.IMAGE,
    ): List<BibleTextBlock> {
        val blocks =
            BibleVersionRendering.introTextBlocks(
                htmlContent = html,
                versionId = 111,
                bookUSFM = "GEN",
                footnoteMode = footnoteMode,
                textColor = Color.Black,
                fonts = fonts,
            )
        assertNotNull(blocks)
        return blocks
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
}

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
