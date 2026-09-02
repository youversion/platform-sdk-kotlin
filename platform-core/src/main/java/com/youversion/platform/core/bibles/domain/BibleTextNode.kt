package com.youversion.platform.core.bibles.domain

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import javax.xml.parsers.SAXParserFactory

/**
 * An enum representing the different types of nodes in the Bible text content tree.
 */
enum class BibleTextNodeType {
    BLOCK,
    TABLE,
    ROW,
    CELL,
    TEXT,
    SPAN,
    ROOT,
}

/**
 * A data class representing a single node in a parsed HTML/XML-like tree.
 * This structure is created by parsing raw HTML content from the Bible API.
 */
data class BibleTextNode(
    val name: String,
    var text: String = "",
    val children: MutableList<BibleTextNode> = mutableListOf(),
    val classes: List<String> = listOf(),
    val attributes: Map<String, String> = mapOf(),
) {
    // This internal property is a transient helper for the parser and not part of the final data model.
    internal var textSegments: MutableList<String> = mutableListOf()

    val type: BibleTextNodeType
        get() =
            when (name) {
                "div", "block" -> BibleTextNodeType.BLOCK
                "table" -> BibleTextNodeType.TABLE
                "tr" -> BibleTextNodeType.ROW
                "td" -> BibleTextNodeType.CELL
                "text" -> BibleTextNodeType.TEXT
                "span" -> BibleTextNodeType.SPAN
                "root" -> BibleTextNodeType.ROOT
                else -> throw IllegalArgumentException("Unknown BibleTextNode type: $name")
            }

    companion object {
        private val supportedElementNames =
            setOf("block", "div", "root", "span", "table", "td", "text", "tr")

        private val htmlVoidElementNames =
            setOf(
                "area",
                "base",
                "br",
                "col",
                "embed",
                "hr",
                "img",
                "input",
                "link",
                "meta",
                "param",
                "source",
                "track",
                "wbr",
            )

        private val voidElementRegex = Regex("""<([A-Za-z][A-Za-z0-9:-]*)([^<>]*)>""")

        private val multipleSpacesRegex = Regex(" {2,}")

        /**
         * Parses a string of HTML content into a root BibleTextNode.
         *
         * @param html The raw HTML string to parse.
         * @return The root node of the parsed tree, or null if parsing fails.
         * @throws org.xml.sax.SAXException if there is an XML parsing error.
         */
        @JvmStatic
        fun parse(html: String): BibleTextNode? {
            val sanitized = sanitizeHTMLForXML(html)
            val data = sanitized.toByteArray(Charsets.UTF_8)

            val factory = SAXParserFactory.newInstance()
            val parser = factory.newSAXParser()
            val handler = SaxParserHandler()

            val inputSource = InputSource(ByteArrayInputStream(data))
            inputSource.encoding = "UTF-8"

            parser.parse(inputSource, handler)

            return handler.parsedRoot
        }

        /**
         * Performs a best-effort transformation to make non-compliant HTML
         * parsable by an XML parser.
         */
        private fun sanitizeHTMLForXML(html: String): String {
            var s = html

            // Self-close HTML void elements if they appear unclosed
            s = selfCloseHTMLVoidElements(s)

            // Decode common HTML named entities to Unicode characters
            val entityMap =
                mapOf(
                    "&nbsp;" to " ",
                    "&mdash;" to "—",
                    "&ndash;" to "–",
                    "&hellip;" to "…",
                    "&rsquo;" to "’",
                    "&lsquo;" to "‘",
                    "&rdquo;" to "”",
                    "&ldquo;" to "“",
                    "&copy;" to "©",
                    "&trade;" to "™",
                )
            entityMap.forEach { (key, value) ->
                s = s.replace(key, value)
            }

            // Wrap with a root element to guarantee a single top-level node for the parser
            return "<root>$s</root>"
        }

        private fun selfCloseHTMLVoidElements(html: String): String =
            voidElementRegex.replace(html) { match ->
                val elementName = match.groupValues[1].lowercase()
                val tag = match.value
                if (elementName in htmlVoidElementNames && !tag.endsWith("/>")) {
                    tag.dropLast(1) + "/>"
                } else {
                    tag
                }
            }
    }

    /**
     * The SAX parser handler that builds the BibleTextNode tree during parsing.
     * This is the Kotlin/Java equivalent of the XMLParserDelegate in Swift.
     */
    private class SaxParserHandler : DefaultHandler() {
        private val parserRoot = BibleTextNode(name = "__parser-root__")
        private val stack = mutableListOf(parserRoot)

        val parsedRoot: BibleTextNode?
            get() = parserRoot.children.firstOrNull()

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String,
            attributes: Attributes,
        ) {
            val attributeMap =
                (0 until attributes.length).associate {
                    attributes.getQName(it) to attributes.getValue(it)
                }

            val classes =
                attributeMap["class"]
                    ?.split(Regex("\\s+")) // Split by one or more whitespace characters
                    ?.filter { it.isNotBlank() } ?: emptyList()

            val filteredAttributes = attributeMap.filterKeys { it != "class" }

            val node = BibleTextNode(name = qName.lowercase(), classes = classes, attributes = filteredAttributes)
            stack.last().children.add(node)
            stack.add(node)
        }

        override fun characters(
            ch: CharArray,
            start: Int,
            length: Int,
        ) {
            val foundString = String(ch, start, length)

            // Collapse multiple whitespace characters into a single space, like HTML does.
            val segment = foundString.replace(Regex("\\s+"), " ")
            if (segment.isEmpty()) return

            val current = stack.last()

            // Only preserve a standalone space when the previous sibling is a span or text node.
            // This prevents spurious leading spaces from HTML indentation while preserving the
            // space between adjacent inline elements like <span class="w">.
            if (segment == " ") {
                val previousChild = current.children.lastOrNull() ?: return
                if (previousChild.type != BibleTextNodeType.SPAN && previousChild.type != BibleTextNodeType.TEXT) return
            }

            // Coalesce adjacent text nodes for efficiency.
            if (current.children.lastOrNull()?.type == BibleTextNodeType.TEXT) {
                val lastChild = current.children.last()
                // Collapse the seam between coalesced segments so removed elements
                // (e.g. an ignored <br>) don't leave a double space behind.
                val joined = (lastChild.text + segment).replace(multipleSpacesRegex, " ")
                lastChild.text = joined
                lastChild.textSegments = mutableListOf(joined)
            } else {
                // This is a new text node.
                val textNode = BibleTextNode(name = "text", text = segment)
                textNode.textSegments = mutableListOf(segment)
                current.children.add(textNode)
            }
        }

        override fun endElement(
            uri: String?,
            localName: String?,
            qName: String?,
        ) {
            val node = stack.removeAt(stack.lastIndex)

            // Drop unsupported elements from the tree but keep their children,
            // hoisting them into the parent so their text still renders.
            if (node.name !in supportedElementNames) {
                val parent = stack.last()
                parent.children.remove(node)
                parent.children.addAll(node.children)
            }
        }
    }
}
