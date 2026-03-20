package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BibleVersionRenderingStructureTests {
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
            assertTrue(tableBlock.rows[1][0].text.contains("Cell C"))
            assertTrue(tableBlock.rows[1][1].text.contains("Cell D"))
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
            val outerBlock = blocks.first { it.text.text.contains("Outer text.") }
            val innerBlock = blocks.first { it.text.text.contains("Inner text.") }
            assertFalse(outerBlock.text.text.contains("Inner text."))
            assertFalse(innerBlock.text.text.contains("Outer text."))

            val outerRefs =
                outerBlock.text.getStringAnnotations(
                    BibleReferenceAttribute.NAME,
                    0,
                    outerBlock.text.length,
                )
            assertTrue(outerRefs.any { it.item.endsWith(":1") })

            val innerRefs =
                innerBlock.text.getStringAnnotations(
                    BibleReferenceAttribute.NAME,
                    0,
                    innerBlock.text.length,
                )
            assertTrue(innerRefs.any { it.item.endsWith(":2") })

            val outerParagraphStyle =
                outerBlock.text.paragraphStyles
                    .first()
                    .item
            assertTrue(outerParagraphStyle.textIndent!!.firstLine.value > 0)
            val innerParagraphStyle =
                innerBlock.text.paragraphStyles
                    .first()
                    .item
            assertTrue(innerParagraphStyle.textIndent!!.firstLine.value > 0)
        }
}
