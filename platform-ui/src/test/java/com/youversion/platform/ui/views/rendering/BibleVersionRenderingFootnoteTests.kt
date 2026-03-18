package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.AnnotatedString
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleVersionRenderingFootnoteTests {
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
            val markerIndex = allText.indexOf("*")
            assertTrue(markerIndex >= 0)
            val textBeforeMarker = allText.indexOf("Some text")
            assertTrue(textBeforeMarker < markerIndex)

            val allFootnotes = blocks.flatMap { it.footnotes }
            assertTrue(allFootnotes.isNotEmpty())
            assertTrue(allFootnotes.any { it.text.contains("footnote body") })

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
    fun `footnote in INLINE mode wraps footnote text in brackets`() =
        runTest {
            val blocks =
                renderBlocks(
                    FOOTNOTE_VERSE_HTML,
                    FULL_CHAPTER_REF,
                    footnoteMode = BibleTextFootnoteMode.INLINE,
                )
            val allText = blocks.joinToString("") { it.text.text }
            val openBracket = allText.indexOf("[")
            val closeBracket = allText.indexOf("]")
            assertTrue(openBracket >= 0)
            assertTrue(closeBracket > openBracket)
            val bracketContent = allText.substring(openBracket + 1, closeBracket)
            assertTrue(bracketContent.contains("footnote body"))

            val allFootnotes = blocks.flatMap { it.footnotes }
            assertEquals(0, allFootnotes.size)

            val hasFootnoteAnnotation =
                blocks.any { block ->
                    block.text
                        .getStringAnnotations(
                            BibleTextCategoryAttribute.NAME,
                            0,
                            block.text.length,
                        ).any {
                            it.item == BibleTextCategory.FOOTNOTE_IMAGE.name ||
                                it.item == BibleTextCategory.FOOTNOTE_MARKER.name
                        }
                }
            assertFalse(hasFootnoteAnnotation)
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

            val hasFootnoteAnnotation =
                blocks.any { block ->
                    block.text
                        .getStringAnnotations(
                            BibleTextCategoryAttribute.NAME,
                            0,
                            block.text.length,
                        ).any {
                            it.item == BibleTextCategory.FOOTNOTE_IMAGE.name ||
                                it.item == BibleTextCategory.FOOTNOTE_MARKER.name
                        }
                }
            assertFalse(hasFootnoteAnnotation)
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
}
