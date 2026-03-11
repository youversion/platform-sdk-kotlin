package com.youversion.platform.core.bibles.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BibleTextNodeTests {
    @Test
    fun `parse should correctly handle complex div and span structure`() {
        val html =
            "<div><div class=\"q1\"><span class=\"yv-v\" v=\"1\">" +
                "</span><span class=\"yv-vlbl\">1</span>" +
                "Praise the <span class=\"nd\">Lord</span>, all you nations;</div></div>"

        // 1. Parse the HTML
        val root = BibleTextNode.parse(html)
        assertNotNull(root)

        // 2. Validate the structure, level by level
        assertEquals(BibleTextNodeType.ROOT, root.type)
        assertEquals(1, root.children.size)

        val outer = root.children.first()
        assertEquals(BibleTextNodeType.BLOCK, outer.type)

        val inner = outer.children.first()
        assertEquals(BibleTextNodeType.BLOCK, inner.type)
        assertTrue(inner.classes.contains("q1"))

        // children: [span.yv-v, span.yv-vlbl, text("Praise the "), span.nd, text(", all you nations;")]
        assertEquals(5, inner.children.size)

        // 3. Validate each child node meticulously
        val verseMarker = inner.children[0]
        assertEquals(BibleTextNodeType.SPAN, verseMarker.type)
        assertTrue(verseMarker.classes.contains("yv-v"))
        assertEquals("1", verseMarker.attributes["v"])
        assertTrue(verseMarker.text.isEmpty()) // In our parser, text is empty if no text node exists

        val verseLabel = inner.children[1]
        assertEquals(BibleTextNodeType.SPAN, verseLabel.type)
        assertTrue(verseLabel.classes.contains("yv-vlbl"))
        assertEquals(1, verseLabel.children.size)
        val verseLabelText = verseLabel.children[0]
        assertEquals(BibleTextNodeType.TEXT, verseLabelText.type)
        assertEquals("1", verseLabelText.text)

        val textBefore = inner.children[2]
        assertEquals(BibleTextNodeType.TEXT, textBefore.type)
        assertEquals("Praise the ", textBefore.text)

        val nameDivine = inner.children[3]
        assertEquals(BibleTextNodeType.SPAN, nameDivine.type)
        assertTrue(nameDivine.classes.contains("nd"))
        assertEquals(1, nameDivine.children.size)
        val nameDivineText = nameDivine.children[0]
        assertEquals(BibleTextNodeType.TEXT, nameDivineText.type)
        assertEquals("Lord", nameDivineText.text)

        val textAfter = inner.children[4]
        assertEquals(BibleTextNodeType.TEXT, textAfter.type)
        assertEquals(", all you nations;", textAfter.text)
    }

    @Test
    fun `parse should preserve space between adjacent w spans`() {
        val html = """<div><div class="p"><span class="w">word1</span> <span class="w">word2</span></div></div>"""
        val root = BibleTextNode.parse(html)
        assertNotNull(root)
        val inner =
            root.children
                .first()
                .children
                .first()
        // Expecting: [span.w, text(" "), span.w]
        assertEquals(3, inner.children.size)
        assertEquals(BibleTextNodeType.TEXT, inner.children[1].type)
        assertEquals(" ", inner.children[1].text)
    }

    @Test
    fun `parse should not produce leading whitespace text node at the start of a block`() {
        val html =
            """
            <div>
              <div class="p">
                <span class="w">In</span> <span class="w">the</span>
              </div>
            </div>
            """.trimIndent()

        val root = BibleTextNode.parse(html)
        assertNotNull(root)
        val inner =
            root.children
                .first()
                .children
                .first()
        assertEquals(BibleTextNodeType.SPAN, inner.children.first().type)
    }

    @Test
    fun `parse should contain expected text from Genesis intro`() {
        val html = """
            <div>
              <div class="pi">
                <span class="yv-v" v="1"></span>
                <span class="yv-vlbl">1</span>
                In the beginning, God created the heavens and the earth.
              </div>
            </div>
        """

        val root = BibleTextNode.parse(html)
        assertNotNull(root)

        fun collectTexts(node: BibleTextNode): List<String> {
            val texts = mutableListOf<String>()
            if (node.type == BibleTextNodeType.TEXT) {
                val trimmed = node.text.trim()
                if (trimmed.isNotEmpty()) {
                    texts.add(trimmed)
                }
            }
            for (child in node.children) {
                texts.addAll(collectTexts(child))
            }
            return texts
        }

        // Execute the helper and assert the result
        val collectedTexts = collectTexts(root)
        val fullSentence = collectedTexts.joinToString(" ")

        // We check if the full sentence contains the expected text.
        // Or we could check if any individual segment contains it.
        assertTrue(
            fullSentence.contains("In the beginning, God created the heavens and the earth."),
        )
    }

    @Test
    fun `parse should correctly handle table with rows and cells`() {
        val html = """<table><tr><td>Cell 1</td><td>Cell 2</td></tr></table>"""
        val root = BibleTextNode.parse(html)
        assertNotNull(root)

        val table = root.children.first()
        assertEquals(BibleTextNodeType.TABLE, table.type)
        assertEquals(1, table.children.size)

        val row = table.children.first()
        assertEquals(BibleTextNodeType.ROW, row.type)
        assertEquals(2, row.children.size)

        val cell1 = row.children[0]
        assertEquals(BibleTextNodeType.CELL, cell1.type)
        assertEquals("Cell 1", cell1.children.first().text)

        val cell2 = row.children[1]
        assertEquals(BibleTextNodeType.CELL, cell2.type)
        assertEquals("Cell 2", cell2.children.first().text)
    }

    @Test
    fun `type returns BLOCK for block name`() {
        val node = BibleTextNode(name = "block")
        assertEquals(BibleTextNodeType.BLOCK, node.type)
    }

    @Test
    fun `type throws IllegalArgumentException for unknown node name`() {
        val node = BibleTextNode(name = "unknown")
        assertFailsWith<IllegalArgumentException> {
            node.type
        }
    }

    @Test
    fun `parse should merge adjacent text segments into one text node`() {
        val html = """<div>Hello &amp; world</div>"""
        val root = BibleTextNode.parse(html)
        assertNotNull(root)
        val block = root.children.first()
        assertEquals(1, block.children.size)
        assertEquals("Hello & world", block.children.first().text)
    }

    @Test
    fun `parse should filter blank entries from class attribute`() {
        val html = """<div class="  foo   bar  "><span>text</span></div>"""
        val root = BibleTextNode.parse(html)
        assertNotNull(root)
        val block = root.children.first()
        assertEquals(listOf("foo", "bar"), block.classes)
    }

    @Test
    fun `parse should not add space when previous sibling is not span or text`() {
        val html = """<div><div class="p"></div> <span class="w">word</span></div>"""
        val root = BibleTextNode.parse(html)
        assertNotNull(root)
        val outer = root.children.first()
        assertEquals(2, outer.children.size)
        assertEquals(BibleTextNodeType.BLOCK, outer.children[0].type)
        assertEquals(BibleTextNodeType.SPAN, outer.children[1].type)
    }
}
