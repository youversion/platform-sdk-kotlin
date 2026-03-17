package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.ui.views.BibleTextFonts
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BibleVersionRenderingTests {
    private val fonts = BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 16.sp)

    private suspend fun renderBlocks(
        html: String,
        reference: BibleReference,
        renderHeadlines: Boolean = true,
        renderVerseNumbers: Boolean = true,
        footnoteMode: BibleTextFootnoteMode = BibleTextFootnoteMode.NONE,
        footnoteMarker: AnnotatedString? = null,
        textColor: Color = Color.Black,
        wocColor: Color = Color.Red,
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
                renderVerseNumbers = renderVerseNumbers,
                renderHeadlines = renderHeadlines,
                footnoteMode = footnoteMode,
                footnoteMarker = footnoteMarker,
                textColor = textColor,
                wocColor = wocColor,
                fonts = fonts,
            )

        assertNotNull(blocks)
        return blocks
    }

    private suspend fun renderBlocksNullable(
        html: String,
        reference: BibleReference,
    ): List<BibleTextBlock>? {
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

        return BibleVersionRendering.textBlocks(
            bibleChapterRepository = repository,
            reference = reference,
            footnoteMode = BibleTextFootnoteMode.NONE,
            textColor = Color.Black,
            fonts = fonts,
        )
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

    // ----- footnote modes

    @Test
    fun `footnote in IMAGE mode inserts marker and populates footnotes`() =
        runTest {
            val blocks =
                renderBlocks(
                    FOOTNOTE_VERSE_HTML,
                    FULL_CHAPTER_REF,
                    footnoteMode = BibleTextFootnoteMode.IMAGE,
                )
            val allFootnotes = blocks.flatMap { it.footnotes }
            assertTrue(allFootnotes.isNotEmpty())
            assertTrue(allFootnotes.any { it.text.contains("footnote body") })

            val hasFootnoteAnnotation =
                blocks.any { block ->
                    block.text
                        .getStringAnnotations(
                            BibleTextCategoryAttribute.NAME,
                            0,
                            block.text.length,
                        ).any { it.item == BibleTextCategory.FOOTNOTE_IMAGE.name }
                }
            assertTrue(hasFootnoteAnnotation)
        }

    @Test
    fun `footnote in LETTERS mode uses letter markers`() =
        runTest {
            val blocks =
                renderBlocks(
                    FOOTNOTE_VERSE_HTML,
                    FULL_CHAPTER_REF,
                    footnoteMode = BibleTextFootnoteMode.LETTERS,
                )
            val allText = blocks.joinToString("") { it.text.text }
            assertTrue(allText.contains("\u00A0a "))

            val hasMarkerAnnotation =
                blocks.any { block ->
                    block.text
                        .getStringAnnotations(
                            BibleTextCategoryAttribute.NAME,
                            0,
                            block.text.length,
                        ).any { it.item == BibleTextCategory.FOOTNOTE_MARKER.name }
                }
            assertTrue(hasMarkerAnnotation)
        }

    @Test
    fun `footnote in MARKER mode inserts custom marker`() =
        runTest {
            val marker = AnnotatedString("*")
            val blocks =
                renderBlocks(
                    FOOTNOTE_VERSE_HTML,
                    FULL_CHAPTER_REF,
                    footnoteMode = BibleTextFootnoteMode.MARKER,
                    footnoteMarker = marker,
                )
            val allText = blocks.joinToString("") { it.text.text }
            assertTrue(allText.contains("*"))
            val allFootnotes = blocks.flatMap { it.footnotes }
            assertTrue(allFootnotes.isNotEmpty())
        }

    @Test
    fun `footnote in INLINE mode wraps footnote text in brackets`() =
        runTest {
            val blocks =
                renderBlocks(
                    FOOTNOTE_VERSE_HTML,
                    FULL_CHAPTER_REF,
                    footnoteMode = BibleTextFootnoteMode.INLINE,
                )
            val allText = blocks.joinToString("") { it.text.text }
            assertTrue(allText.contains("["))
            assertTrue(allText.contains("]"))
            val allFootnotes = blocks.flatMap { it.footnotes }
            assertEquals(0, allFootnotes.size)
        }

    @Test
    fun `footnote in NONE mode replaces footnote with space`() =
        runTest {
            val blocks =
                renderBlocks(
                    FOOTNOTE_VERSE_HTML,
                    FULL_CHAPTER_REF,
                    footnoteMode = BibleTextFootnoteMode.NONE,
                )
            val allText = blocks.joinToString("") { it.text.text }
            assertFalse(allText.contains("footnote body"))
            val allFootnotes = blocks.flatMap { it.footnotes }
            assertEquals(0, allFootnotes.size)
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

    // ----- table rendering

    @Test
    fun `table node produces BibleTextBlock with rows`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        <table>
                            <tr><td>Cell A</td><td>Cell B</td></tr>
                            <tr><td>Cell C</td><td>Cell D</td></tr>
                        </table>
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val tableBlock = blocks.firstOrNull { it.rows.isNotEmpty() }
            assertNotNull(tableBlock)
            assertEquals(2, tableBlock.rows.size)
            assertEquals(2, tableBlock.rows.first().size)
            assertTrue(tableBlock.rows[0][0].text.contains("Cell A"))
            assertTrue(tableBlock.rows[0][1].text.contains("Cell B"))
        }

    // ----- cross-references

    @Test
    fun `cross-references with rq class are ignored`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Some text
                        <span class="rq">cross-ref content</span>
                        more text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val allText = blocks.joinToString("") { it.text.text }
            assertFalse(allText.contains("cross-ref content"))
            assertTrue(allText.contains("Some text"))
        }

    // ----- selah

    @Test
    fun `selah with qs class creates separate right-aligned block`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Psalm text here.
                        <span class="qs"><span>Selah</span></span>
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val selahBlock = blocks.firstOrNull { it.text.text.contains("Selah") }
            assertNotNull(selahBlock)
            assertEquals(TextAlign.End, selahBlock.alignment)
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
    fun `empty root node returns empty list`() =
        runTest {
            val result = renderBlocksNullable("<div></div>", FULL_CHAPTER_REF)
            assertNotNull(result)
            assertTrue(result.isEmpty())
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
            assertTrue(blocks.size >= 2)
            assertTrue(blocks.any { it.text.text.contains("Paragraph one.") })
            assertTrue(blocks.any { it.text.text.contains("Paragraph two.") })
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

    // ----- indentation

    @Test
    fun `p class produces first line indent`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
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
            assertTrue(paragraphStyle.textIndent!!.firstLine.value > 0)
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
            assertTrue(paragraphStyle.textIndent!!.restLine.value > 0)
        }

    // ----- alignment

    @Test
    fun `qr class produces end alignment`() =
        runTest {
            val html =
                """
                <div>
                    <div class="qr">
                        <span class="yv-v" v="1"></span>
                        Right aligned text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val block = blocks.first { it.text.text.contains("Right aligned text.") }
            assertEquals(TextAlign.End, block.alignment)
        }

    @Test
    fun `pc class produces center alignment`() =
        runTest {
            val html =
                """
                <div>
                    <div class="pc">
                        <span class="yv-v" v="1"></span>
                        Centered text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val block = blocks.first { it.text.text.contains("Centered text.") }
            assertEquals(TextAlign.Center, block.alignment)
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
                    it.item.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic
                },
            )
        }

    // ----- nested blocks

    @Test
    fun `nested blocks recurse and produce blocks`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Outer text.
                        <div class="p">
                            <span class="yv-v" v="2"></span>
                            Inner text.
                        </div>
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            assertTrue(blocks.any { it.text.text.contains("Outer text.") })
            assertTrue(blocks.any { it.text.text.contains("Inner text.") })
        }

    // ----- cross-references via yv-n + x

    @Test
    fun `cross-references with yv-n and x class are ignored`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Some text
                        <span class="yv-n x"><span>cross ref</span></span>
                        more text.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val allText = blocks.joinToString("") { it.text.text }
            assertFalse(allText.contains("cross ref"))
            assertTrue(allText.contains("Some text"))
        }

    // ----- footnote LETTERS reset per verse

    @Test
    fun `footnote LETTERS mode resets letter counter per verse`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        Text
                        <span class="yv-n f"><span class="fr">1:1</span><span class="ft">fn1</span></span>
                        more
                        <span class="yv-n f"><span class="fr">1:1</span><span class="ft">fn2</span></span>
                        end.
                        <span class="yv-v" v="2"></span>
                        Second
                        <span class="yv-n f"><span class="fr">1:2</span><span class="ft">fn3</span></span>
                        done.
                    </div>
                </div>
                """.trimIndent()

            val blocks =
                renderBlocks(
                    html,
                    FULL_CHAPTER_REF,
                    footnoteMode = BibleTextFootnoteMode.LETTERS,
                )
            val allText = blocks.joinToString("") { it.text.text }
            val firstA = allText.indexOf("\u00A0a ")
            val firstB = allText.indexOf("\u00A0b ")
            val secondA = allText.indexOf("\u00A0a ", firstA + 1)
            assertTrue(firstA >= 0)
            assertTrue(firstB > firstA)
            assertTrue(secondA > firstB)
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

    // ----- additional alignment

    @Test
    fun `qc class produces center alignment`() =
        runTest {
            val html =
                """
                <div>
                    <div class="qc">
                        <span class="yv-v" v="1"></span>
                        Centered poetry.
                    </div>
                </div>
                """.trimIndent()

            val blocks = renderBlocks(html, FULL_CHAPTER_REF)
            val block = blocks.first { it.text.text.contains("Centered poetry.") }
            assertEquals(TextAlign.Center, block.alignment)
        }

    // ----- additional indentation

    @Test
    fun `pi class produces first line and head indent`() =
        runTest {
            val html =
                """
                <div>
                    <div class="pi">
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
            assertTrue(paragraphStyle.textIndent!!.firstLine.value > 0)
        }

    @Test
    fun `li1 class produces head indent`() =
        runTest {
            val html =
                """
                <div>
                    <div class="li1">
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
            assertTrue(paragraphStyle.textIndent!!.restLine.value > 0)
        }

    @Test
    fun `q1 class produces no indent`() =
        runTest {
            val html =
                """
                <div>
                    <div class="q1">
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
            assertEquals(0f, paragraphStyle.textIndent!!.firstLine.value)
            assertEquals(0f, paragraphStyle.textIndent!!.restLine.value)
        }

    // ----- bold font style

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
                    fonts = fonts,
                )

            assertEquals(2, callCount.get())
            assertNotNull(blocks)
            assertTrue(blocks.any { it.text.text.contains("First verse text.") })
        }
}

private val FULL_CHAPTER_REF =
    BibleReference(
        versionId = 111,
        bookUSFM = "GEN",
        chapter = 1,
    )

private val SIMPLE_VERSE_HTML =
    """
    <div>
        <div class="p">
            <span class="yv-v" v="1"></span>
            <span class="yv-vlbl">1</span>
            First verse text.
        </div>
    </div>
    """.trimIndent()

private val FOOTNOTE_VERSE_HTML =
    """
    <div>
        <div class="p">
            <span class="yv-v" v="1"></span>
            Some text
            <span class="yv-n f"><span class="fr">1:1</span><span class="ft">footnote body</span></span>
            after footnote.
        </div>
    </div>
    """.trimIndent()

private val THREE_VERSE_HTML =
    """
    <div>
        <div class="p">
            <span class="yv-v" v="1"></span>
            <span class="yv-vlbl">1</span>
            First.
            <span class="yv-v" v="2"></span>
            <span class="yv-vlbl">2</span>
            Second.
            <span class="yv-v" v="3"></span>
            <span class="yv-vlbl">3</span>
            Third.
        </div>
    </div>
    """.trimIndent()
