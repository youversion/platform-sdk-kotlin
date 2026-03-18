package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BibleVersionRenderingTests {
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

    // ----- verse numbers

    @Test
    fun `verse numbers are rendered when renderVerseNumbers is true`() =
        runTest {
            val blocks =
                renderBlocks(SIMPLE_VERSE_HTML, FULL_CHAPTER_REF, renderVerseNumbers = true)
            val allText = blocks.joinToString("") { it.text.text }
            assertTrue(allText.contains("1\u00A0"))
        }

    @Test
    fun `verse numbers are not rendered when renderVerseNumbers is false`() =
        runTest {
            val blocks =
                renderBlocks(SIMPLE_VERSE_HTML, FULL_CHAPTER_REF, renderVerseNumbers = false)
            val allText = blocks.joinToString("") { it.text.text }
            assertFalse(allText.contains("1\u00A0"))
            assertTrue(allText.contains("First verse text."))
        }

    // ----- verse range filtering

    @Test
    fun `verse range filtering renders only verses in range`() =
        runTest {
            val reference =
                BibleReference(
                    versionId = 111,
                    bookUSFM = "GEN",
                    chapter = 1,
                    verseStart = 2,
                    verseEnd = 2,
                )

            val blocks = renderBlocks(THREE_VERSE_HTML, reference)
            val allText = blocks.joinToString("") { it.text.text }
            assertTrue(allText.contains("Second."))
            assertFalse(allText.contains("First."))
            assertFalse(allText.contains("Third."))
        }

    @Test
    fun `full chapter renders all content`() =
        runTest {
            val blocks = renderBlocks(THREE_VERSE_HTML, FULL_CHAPTER_REF)
            val allText = blocks.joinToString("") { it.text.text }
            assertTrue(allText.contains("First."))
            assertTrue(allText.contains("Second."))
            assertTrue(allText.contains("Third."))
        }

    // ----- edge cases

    @Test
    fun `empty root node returns null`() =
        runTest {
            val result = renderBlocksNullable("", FULL_CHAPTER_REF)
            assertNull(result)
        }

    @Test
    fun `multiple paragraphs produce separate BibleTextBlocks`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Paragraph one.
                    </div>
                    <div class="p">
                        <span class="yv-v" v="2"></span>
                        Paragraph two.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            assertEquals(2, blocks.size)
            assertTrue(blocks[0].text.text.contains("Paragraph one."))
            assertTrue(blocks[1].text.text.contains("Paragraph two."))
        }

    @Test
    fun `double space is collapsed to single space`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        word  word
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val allText = blocks.joinToString("") { it.text.text }
            assertTrue(allText.contains("word word"))
            assertFalse(allText.contains("  "))
        }

    // ----- chapter label

    @Test
    fun `chapter label cl class is skipped`() =
        runTest {
            val html =
                """
                <div>
                    <div class="cl">Chapter One</div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Verse text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val allText = blocks.joinToString("") { it.text.text }
            assertFalse(allText.contains("Chapter One"))
            assertTrue(allText.contains("Verse text."))
        }

    // ----- cache invalidation

    @Test
    fun `cache invalidation re-fetches when children count is zero`() =
        runTest {
            val emptyChildrenHtml = ""
            val validHtml = SIMPLE_VERSE_HTML
            val callCount = AtomicInteger(0)

            val swappingCache =
                object : BibleVersionCache {
                    override val storedVersionIds: List<Int> = emptyList()

                    override suspend fun version(id: Int): BibleVersion? = null

                    override suspend fun chapterContent(reference: BibleReference): String {
                        val count = callCount.incrementAndGet()
                        return if (count == 1) emptyChildrenHtml else validHtml
                    }

                    override suspend fun addVersion(version: BibleVersion) {}

                    override suspend fun addChapterContents(
                        content: String,
                        reference: BibleReference,
                    ) {}

                    override suspend fun removeVersion(versionId: Int) {}

                    override suspend fun removeVersionChapters(versionId: Int) {}

                    override suspend fun removeUnpermittedVersions(permittedIds: Set<Int>) {}

                    override fun versionIsPresent(versionId: Int): Boolean = false

                    override fun chaptersArePresent(versionId: Int): Boolean = false
                }

            val repository =
                BibleChapterRepository(
                    memoryCache = swappingCache,
                    temporaryCache = BibleVersionMemoryCache(),
                    persistentCache = BibleVersionMemoryCache(),
                )

            val blocks =
                BibleVersionRendering.textBlocks(
                    bibleChapterRepository = repository,
                    reference = FULL_CHAPTER_REF,
                    footnoteMode = BibleTextFootnoteMode.NONE,
                    textColor = Color.Black,
                    fonts = RENDERING_TEST_FONTS,
                )

            assertEquals(2, callCount.get())
            assertNotNull(blocks)
            assertTrue(blocks.any { it.text.text.contains("First verse text.") })
        }
}
