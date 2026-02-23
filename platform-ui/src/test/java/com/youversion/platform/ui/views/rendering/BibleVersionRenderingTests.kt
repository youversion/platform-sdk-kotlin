package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.ui.views.BibleTextFonts
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BibleVersionRenderingTests {
    private val fonts = BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 16.sp)

    private suspend fun renderBlocks(
        html: String,
        reference: BibleReference,
        renderHeadlines: Boolean = true,
    ): List<BibleTextBlock> {
        val cache = BibleVersionMemoryCache()
        val chapterReference =
            BibleReference(
                versionId = reference.versionId,
                bookUSFM = reference.bookUSFM,
                chapter = reference.chapter,
            )
        cache.addChapterContents(html, chapterReference)

        val repository =
            BibleChapterRepository(
                memoryCache = cache,
                temporaryCache = BibleVersionMemoryCache(),
                persistentCache = BibleVersionMemoryCache(),
            )

        val blocks =
            BibleVersionRendering.textBlocks(
                bibleChapterRepository = repository,
                reference = reference,
                renderHeadlines = renderHeadlines,
                footnoteMode = BibleTextFootnoteMode.NONE,
                textColor = Color.Black,
                fonts = fonts,
            )

        assertNotNull(blocks)
        return blocks
    }

    private fun List<BibleTextBlock>.hasHeaderContaining(text: String): Boolean =
        any { block ->
            val annotatedText = block.text
            val annotations =
                annotatedText.getStringAnnotations(
                    tag = BibleTextCategoryAttribute.NAME,
                    start = 0,
                    end = annotatedText.length,
                )
            annotations.any { it.item == BibleTextCategory.HEADER.name } &&
                annotatedText.text.contains(text)
        }

    private fun List<BibleTextBlock>.hasScriptureContaining(text: String): Boolean =
        any { block -> block.text.text.contains(text) }

    @Test
    fun `header before first verse in range is rendered`() =
        runTest {
            val html =
                """
                <div>
                    <div class="yv-h s1"><span>The List</span></div>
                    <div class="p">
                        <span class="yv-v" v="5"></span>
                        <span class="yv-vlbl">5</span>
                        Fifth verse text.
                    </div>
                    <div class="p">
                        <span class="yv-v" v="6"></span>
                        <span class="yv-vlbl">6</span>
                        Sixth verse text.
                    </div>
                </div>
                """.trimIndent()

            val reference =
                BibleReference(
                    versionId = 111,
                    bookUSFM = "GEN",
                    chapter = 1,
                    verseStart = 5,
                    verseEnd = 10,
                )

            val blocks = renderBlocks(html, reference)
            assertTrue(blocks.hasHeaderContaining("The List"))
            assertTrue(blocks.hasScriptureContaining("Fifth verse text."))
        }

    @Test
    fun `header in middle of last verse in range is rendered`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="5"></span>
                        <span class="yv-vlbl">5</span>
                        Part one of verse five.
                    </div>
                    <div class="yv-h s1"><span>Mid-Verse Header</span></div>
                    <div class="p">
                        Part two of verse five.
                    </div>
                </div>
                """.trimIndent()

            val reference =
                BibleReference(
                    versionId = 111,
                    bookUSFM = "GEN",
                    chapter = 1,
                    verseStart = 1,
                    verseEnd = 5,
                )

            val blocks = renderBlocks(html, reference)
            assertTrue(blocks.hasHeaderContaining("Mid-Verse Header"))
            assertTrue(blocks.hasScriptureContaining("Part one of verse five."))
            assertTrue(blocks.hasScriptureContaining("Part two of verse five."))
        }

    @Test
    fun `header after last verse in range is not rendered`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="5"></span>
                        <span class="yv-vlbl">5</span>
                        Fifth verse text.
                    </div>
                    <div class="yv-h s1"><span>Next Section</span></div>
                    <div class="p">
                        <span class="yv-v" v="6"></span>
                        <span class="yv-vlbl">6</span>
                        Sixth verse text.
                    </div>
                </div>
                """.trimIndent()

            val reference =
                BibleReference(
                    versionId = 111,
                    bookUSFM = "GEN",
                    chapter = 1,
                    verseStart = 1,
                    verseEnd = 5,
                )

            val blocks = renderBlocks(html, reference)
            assertTrue(blocks.hasScriptureContaining("Fifth verse text."))
            assertFalse(blocks.hasHeaderContaining("Next Section"))
            assertFalse(blocks.hasScriptureContaining("Sixth verse text."))
        }

    @Test
    fun `header before out-of-range verse is not rendered`() =
        runTest {
            val html =
                """
                <div>
                    <div class="yv-h s1"><span>Early Section</span></div>
                    <div class="p">
                        <span class="yv-v" v="3"></span>
                        <span class="yv-vlbl">3</span>
                        Third verse text.
                    </div>
                    <div class="p">
                        <span class="yv-v" v="5"></span>
                        <span class="yv-vlbl">5</span>
                        Fifth verse text.
                    </div>
                </div>
                """.trimIndent()

            val reference =
                BibleReference(
                    versionId = 111,
                    bookUSFM = "GEN",
                    chapter = 1,
                    verseStart = 5,
                    verseEnd = 10,
                )

            val blocks = renderBlocks(html, reference)
            assertFalse(blocks.hasHeaderContaining("Early Section"))
            assertFalse(blocks.hasScriptureContaining("Third verse text."))
            assertTrue(blocks.hasScriptureContaining("Fifth verse text."))
        }

    @Test
    fun `header is not rendered when renderHeadlines is false`() =
        runTest {
            val html =
                """
                <div>
                    <div class="yv-h s1"><span>Visible Header</span></div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        <span class="yv-vlbl">1</span>
                        First verse text.
                    </div>
                </div>
                """.trimIndent()

            val reference =
                BibleReference(
                    versionId = 111,
                    bookUSFM = "GEN",
                    chapter = 1,
                    verseStart = 1,
                    verseEnd = 5,
                )

            val blocks = renderBlocks(html, reference, renderHeadlines = false)
            assertFalse(blocks.hasHeaderContaining("Visible Header"))
            assertTrue(blocks.hasScriptureContaining("First verse text."))
        }
}
