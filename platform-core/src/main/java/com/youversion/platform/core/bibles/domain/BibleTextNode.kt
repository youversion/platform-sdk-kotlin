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

            // Self-close common void elements
            s =
                s
                    .replace("<br>", "<br/>")
                    .replace("<br >", "<br/>")

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

            val node = BibleTextNode(name = qName, classes = classes, attributes = filteredAttributes)
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
            val collapsed = foundString.replace(Regex("\\s+"), " ")
            val core = collapsed.trim()
            if (core.isEmpty()) return

            // Preserve leading/trailing space which might be significant between text segments.
            val leadingSpace = collapsed.firstOrNull()?.isWhitespace() == true
            val trailingSpace = collapsed.lastOrNull()?.isWhitespace() == true && core.length > 1
            var segment = core
            if (leadingSpace) segment = " $segment"
            if (trailingSpace) segment += " "

            val current = stack.last()

            // Coalesce adjacent text nodes for efficiency.
            if (current.children.lastOrNull()?.type == BibleTextNodeType.TEXT) {
                val lastChild = current.children.last()
                lastChild.textSegments.add(segment)
                val joined = lastChild.textSegments.joinToString("")
                lastChild.text = joined
                // Once joined, we can reset textSegments to keep memory usage low.
                lastChild.textSegments = if (joined.isEmpty()) mutableListOf() else mutableListOf(joined)
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
            stack.removeAt(stack.lastIndex)
        }
    }
}
