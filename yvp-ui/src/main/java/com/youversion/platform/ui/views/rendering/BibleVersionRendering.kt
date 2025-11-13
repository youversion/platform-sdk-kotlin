package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleTextNode
import com.youversion.platform.core.bibles.domain.BibleTextNodeType
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.utilities.exceptions.BibleVersionApiException
import com.youversion.platform.ui.views.BibleTextFontOption
import com.youversion.platform.ui.views.BibleTextFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private const val DEBUG_RENDERING = false

/**
 * Provides functionality for rendering Bible references into plain text or rich text blocks
 * for use in Jetpack Compose.
 */
object BibleVersionRendering {
    /**
     * Returns plain text for a Bible reference.
     */
    suspend fun plainTextOf(
        bibleVersionRepository: BibleVersionRepository,
        reference: BibleReference,
    ): String? {
        val fonts = BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 16.sp)
        return try {
            val blocks =
                textBlocks(
                    bibleVersionRepository = bibleVersionRepository,
                    reference = reference,
                    renderVerseNumbers = false,
                    renderHeadlines = false,
                    renderFootnotes = false,
                    fonts = fonts,
                )
            blocks?.joinToString(separator = "\n") { it.text.text }
        } catch (e: BibleVersionApiException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formats Bible data into styled AnnotatedString blocks for Compose.
     */
    suspend fun textBlocks(
        bibleVersionRepository: BibleVersionRepository,
        reference: BibleReference,
        renderVerseNumbers: Boolean = true,
        renderHeadlines: Boolean = true,
        renderFootnotes: Boolean = false,
        footnoteMarker: AnnotatedString? = null,
        textColor: Color = Color.Unspecified,
        wocColor: Color = Color.Red,
        fonts: BibleTextFonts,
    ): List<BibleTextBlock>? =
        withContext(Dispatchers.IO) {
            val chapterRef =
                BibleReference(
                    reference.versionId,
                    reference.bookUSFM,
                    reference.chapter,
                )

            val rootNode: BibleTextNode? =
                try {
                    var data = bibleVersionRepository.chapter(reference = chapterRef)
                    val node = BibleTextNode.parse(data)
                    if (node?.children?.count() == 0) {
                        bibleVersionRepository.removeVersionChapters(reference.versionId)
                        data = bibleVersionRepository.chapter(reference = chapterRef)
                        BibleTextNode.parse(data)
                    } else {
                        node
                    }
                } catch (e: BibleVersionApiException) {
                    throw e
                } catch (_: Exception) {
                    return@withContext null
                }

            if (rootNode?.children?.isEmpty() == true) {
                return@withContext null
            }

            val stateIn =
                StateIn(
                    versionId = reference.versionId,
                    bookUSFM = reference.bookUSFM,
                    currentChapter = reference.chapter,
                    fromVerse = reference.verseStart ?: 1,
                    toVerse = reference.verseEnd ?: 999,
                    renderVerseNumbers = renderVerseNumbers,
                    renderHeadlines = renderHeadlines,
                    renderFootnotes = renderFootnotes,
                    footnoteMarker = footnoteMarker,
                    textColor = textColor,
                    wocColor = wocColor,
                    fonts = fonts,
                )

            val resultBlocks = mutableListOf<BibleTextBlock>()
            val stateDown =
                StateDown(
                    currentFont = BibleTextFontOption.TEXT,
                    textCategory = BibleTextCategory.SCRIPTURE,
                )
            val stateUp =
                StateUp(
                    rendering = stateIn.fromVerse <= 1,
                    versionId = reference.versionId,
                    bookUSFM = reference.bookUSFM,
                    chapter = reference.chapter,
                    verse = 0,
                )

            rootNode?.children?.first()?.let {
                handleNodeBlock(
                    node = it,
                    stateIn = stateIn,
                    parentStateDown = stateDown,
                    stateUp = stateUp,
                    resultBlocks = resultBlocks,
                )
            }
            resultBlocks
        }

    private fun traceLog(
        node: BibleTextNode,
        stateDown: StateDown,
    ) {
        if (!DEBUG_RENDERING) return
        val nodeType = node.type.name.lowercase()
        val indent = "_".repeat(stateDown.nodeDepth)
        println("$indent ${nodeType.padEnd(6)} ${node.classes} ${node.text}")
    }

    private fun assertionFailed(
        message: String,
        detail: Any? = null,
    ) {
        if (!DEBUG_RENDERING) return
        println("ASSERTION FAILED: $message${detail?.toString() ?: ""}")
    }

    private fun createBlock(
        stateDown: StateDown,
        stateUp: StateUp,
        marginTop: Dp,
    ): BibleTextBlock {
        val text =
            buildAnnotatedString {
                withStyle(
                    style =
                        ParagraphStyle(
                            textIndent =
                                TextIndent(
                                    firstLine = stateUp.firstLineHeadIndent,
                                    restLine = stateUp.headIndent,
                                ),
                        ),
                ) {
                    append(stateUp.textBuilder.toAnnotatedString())
                }
            }

        val block =
            BibleTextBlock(
                text = text,
                chapter = stateUp.chapter,
                headIndent = stateUp.headIndent,
                marginTop = marginTop,
                alignment = stateDown.alignment,
                footnotes = stateUp.footnotes.toList(),
            )
        stateUp.footnotes.clear()
        return block
    }

    // --- Node Handling Logic ---

    private fun handleBlockChild(
        node: BibleTextNode,
        stateIn: StateIn,
        parentStateDown: StateDown,
        stateUp: StateUp,
    ) {
        val stateDown = parentStateDown.copy().apply { nodeDepth = parentStateDown.nodeDepth + 1 }
        traceLog(node, stateDown)

        if (node.type != BibleTextNodeType.SPAN && node.type != BibleTextNodeType.TEXT) {
            assertionFailed("handleBlockChild: unexpected type: ", node.type)
        }

        interpretTextAttr(node, stateIn, stateDown, stateUp)

        if (stateUp.rendering && node.text.isNotEmpty()) {
            val text = if (node.text == "  ") " " else node.text
            val style =
                stateIn.fonts.styleFor(stateDown.currentFont).let {
                    if (stateDown.woc) it.copy(color = stateIn.wocColor) else it
                }
            stateUp.append(text, style, stateDown.textCategory)
        }

        if (node.classes.contains("yv-vlbl") || node.classes.contains("vlbl")) {
            if (stateUp.rendering && stateIn.renderVerseNumbers && node.children.isNotEmpty()) {
                val text = node.children.first().text
                val maybeSpace = if (stateUp.isTextEmpty() || stateUp.endsWithSpace()) "" else " "
                val verseNumText = "$maybeSpace$text\u00A0" // non-breaking space
                val verseNumStyle =
                    stateIn.fonts.styleFor(BibleTextFontOption.VERSE_NUM).copy(
                        baselineShift = stateIn.fonts.verseNumBaselineShift,
                        color = stateIn.textColor.copy(alpha = stateIn.fonts.verseNumOpacity),
                    )
                stateUp.append(verseNumText, verseNumStyle, BibleTextCategory.VERSE_LABEL)
            }
        } else if (node.classes.contains("yv-n") && node.classes.contains("f")) {
            // Handle footnote node
            if (stateUp.rendering) {
                handleFootnoteNode(node, stateIn, parentStateDown, stateUp)
            }
        } else if (node.classes.contains("rq") || (node.classes.contains("yv-n") && node.classes.contains("x"))) {
            // Cross-reference, currently ignored
        } else {
            node.children.forEach { child ->
                handleBlockChild(child, stateIn, stateDown, stateUp)
            }
        }
    }

    private fun handleFootnoteNode(
        node: BibleTextNode,
        stateIn: StateIn,
        parentStateDown: StateDown,
        stateUp: StateUp,
    ) {
        // If not rendering footnotes just add a space and return
        if (!stateIn.renderFootnotes) {
            val style = stateIn.fonts.styleFor(parentStateDown.currentFont)
            stateUp.append(text = " ", style = style, category = parentStateDown.textCategory)
            return
        }

        // Create a new StateDown for the footnote context.
        var stateDown =
            parentStateDown.copy().apply {
                nodeDepth = parentStateDown.nodeDepth + 1
                textCategory = BibleTextCategory.FOOTNOTE_TEXT
            }

        if (stateIn.footnoteMarker != null) {
            // Case 1: Footnotes are displayed with a marker and collected separately.
            // Append the marker (e.g., a superscript number) to the main text builder.
            stateUp.appendFootnote(stateIn.footnoteMarker, BibleTextCategory.FOOTNOTE_MARKER)

            // Now, create a new, temporary StateUp object to build the footnote's content.
            // This isolates the footnote's AnnotatedString from the main text.
            val footState =
                StateUp(
                    rendering = true,
                    versionId = stateUp.versionId,
                    bookUSFM = stateUp.bookUSFM,
                    chapter = stateUp.chapter,
                    verse = stateUp.verse,
                    // Other properties like indents are irrelevant here.
                )

            // Ensure the font for the footnote text is correctly set.
            stateDown =
                stateDown.copy().apply {
                    currentFont = BibleTextFontOption.FOOTNOTE
                }

            // Recursively process all children of the footnote node to build its content.
            for (child in node.children) {
                handleBlockChild(
                    node = child,
                    stateIn = stateIn,
                    parentStateDown = stateDown,
                    stateUp = footState,
                )
            }

            // Once the footnote's text is fully built, add the resulting AnnotatedString
            // to the main state's list of footnotes.
            if (!footState.isTextEmpty()) {
                stateUp.footnotes.add(footState.textBuilder.toAnnotatedString())
            }
        } else {
            // Case 2: Footnotes are rendered directly inline with the scripture text.
            // Set the font for the inline footnote text.
            stateDown =
                stateDown.copy().apply {
                    currentFont = BibleTextFontOption.FOOTNOTE
                }

            val defaultStyle = stateIn.fonts.styleFor(BibleTextFontOption.TEXT)
            stateUp.append("[", defaultStyle, BibleTextCategory.SCRIPTURE)

            // Recursively process the footnote's children, but append the result
            // directly to the main text builder (`stateUp`).
            for (child in node.children) {
                handleBlockChild(
                    node = child,
                    stateIn = stateIn,
                    parentStateDown = stateDown,
                    stateUp = stateUp,
                )
            }

            stateUp.append("]", defaultStyle, BibleTextCategory.SCRIPTURE)
        }
    }

    // --- Table, Row, and Cell Handling Functions ---

    /**
     * Processes a <table> node, collecting all its rows and creating a special
     * table-based BibleTextBlock.
     */
    private fun handleNodeTable(
        node: BibleTextNode,
        stateIn: StateIn,
        parentStateDown: StateDown,
        stateUp: StateUp,
        ret: MutableList<BibleTextBlock>, // The list of final blocks to render
    ) {
        val stateDown =
            parentStateDown.copy().apply {
                nodeDepth = parentStateDown.nodeDepth + 1
            }
        traceLog(node, stateDown)

        val rows = mutableListOf<List<AnnotatedString>>()

        if (node.classes.isNotEmpty()) {
            assertionFailed("unexpected classes for a table node: ", node.classes)
        }

        // Iterate through children of the <table>, expecting them to be <tr> (row) nodes.
        for (child in node.children) {
            if (child.type == BibleTextNodeType.ROW) {
                val row =
                    handleNodeRow(
                        node = child,
                        stateIn = stateIn,
                        parentStateDown = stateDown,
                        stateUp = stateUp,
                    )
                if (row.isNotEmpty()) {
                    rows.add(row)
                }
            } else {
                assertionFailed("unexpected child of a table: ", child.type)
            }
        }

        // If we successfully collected rows, create a BibleTextBlock to hold them.
        // This block has no primary `text`, but instead contains the `rows` data.
        if (rows.isNotEmpty()) {
            ret.add(
                BibleTextBlock(
                    text = AnnotatedString(""), // Table blocks have no primary text
                    chapter = stateUp.chapter,
                    headIndent = TextUnit(0f, TextUnitType.Sp),
                    marginTop = 10.dp, // A default margin for tables
                    alignment = TextAlign.Start,
                    footnotes = stateUp.footnotes.toList(), // Capture any footnotes found so far
                    rows = rows,
                ),
            )
            // Clear footnotes after they've been assigned to a block.
            stateUp.footnotes.clear()
        }
    }

    /**
     * Processes a <tr> (table row) node, collecting all its cells.
     *
     * @return A list of AnnotatedStrings, where each string is the content of one cell.
     */
    private fun handleNodeRow(
        node: BibleTextNode,
        stateIn: StateIn,
        parentStateDown: StateDown,
        stateUp: StateUp,
    ): List<AnnotatedString> {
        val stateDown =
            parentStateDown.copy().apply {
                nodeDepth = parentStateDown.nodeDepth + 1
            }
        traceLog(node, stateDown)

        val thisRowCells = mutableListOf<AnnotatedString>()

        // Iterate through children of the <tr>, expecting them to be <td> (cell) nodes.
        for (child in node.children) {
            if (child.type == BibleTextNodeType.CELL) {
                // Process the cell. This will populate stateUp.textBuilder.
                handleNodeCell(
                    node = child,
                    stateIn = stateIn,
                    parentStateDown = stateDown,
                    stateUp = stateUp,
                )

                if (stateUp.rendering) {
                    // Finalize the cell's text and add it to our list for this row.
                    val cellText = stateUp.textBuilder.toAnnotatedString().trimTrailingWhitespace()
                    thisRowCells.add(cellText)

                    // IMPORTANT: Clear the text builder to prepare for the next cell.
                    stateUp.clearText()
                }
            } else {
                assertionFailed("unexpected child of a table row: ", child.type)
            }
        }

        return thisRowCells
    }

    /**
     * Processes a <td> (table cell) node by rendering its text content.
     */
    private fun handleNodeCell(
        node: BibleTextNode,
        stateIn: StateIn,
        parentStateDown: StateDown,
        stateUp: StateUp,
    ) {
        val stateDown =
            parentStateDown.copy().apply {
                nodeDepth = parentStateDown.nodeDepth + 1
                currentFont = BibleTextFontOption.TEXT // Reset font to default for table cells
            }
        traceLog(node, stateDown)

        // A cell's children should only be simple text or spans.
        for (child in node.children) {
            if (child.type == BibleTextNodeType.SPAN || child.type == BibleTextNodeType.TEXT) {
                // Delegate to handleBlockChild, which populates stateUp.textBuilder.
                handleBlockChild(
                    node = child,
                    stateIn = stateIn,
                    parentStateDown = stateDown,
                    stateUp = stateUp,
                )
            } else {
                assertionFailed("unexpected child of a table cell: ", child.type)
            }
        }
    }

    private fun handleNodeBlock(
        node: BibleTextNode,
        stateIn: StateIn,
        parentStateDown: StateDown,
        stateUp: StateUp,
        resultBlocks: MutableList<BibleTextBlock>,
    ) {
        var stateDown = parentStateDown.copy().apply { nodeDepth = parentStateDown.nodeDepth + 1 }
        var marginTop: Dp = 0.dp
        stateDown = stateDown.copy().apply { currentFont = BibleTextFontOption.TEXT }

        if (node.type != BibleTextNodeType.BLOCK) {
            assertionFailed("handleNodeBlock was given non-block: ", node.type)
            return
        }
        traceLog(node, stateDown)
        if (node.classes.contains("cl")) { // Chapter label, handled by UI, so ignore.
            return
        }

        interpretBlockClasses(
            classes = node.classes,
            stateIn = stateIn,
            stateDown = stateDown,
            stateUp = stateUp,
        ) { newMargin ->
            marginTop = newMargin
        }

        for (child in node.children) {
            if (child.type == BibleTextNodeType.BLOCK || child.type == BibleTextNodeType.TABLE) {
                if (!stateUp.isTextEmpty()) {
                    if (stateUp.rendering) {
                        resultBlocks.add(createBlock(stateDown, stateUp, marginTop))
                    }
                    stateUp.clearText()
                }
                if (child.type == BibleTextNodeType.TABLE) {
                    handleNodeTable(
                        node = child,
                        stateIn = stateIn,
                        parentStateDown = stateDown,
                        stateUp = stateUp,
                        ret = resultBlocks,
                    )
                } else { // It's a nested block
                    handleNodeBlock(
                        node = child,
                        stateIn = stateIn,
                        parentStateDown = stateDown,
                        stateUp = stateUp,
                        resultBlocks = resultBlocks,
                    )
                }
            } else {
                handleBlockChild(
                    node = child,
                    stateIn = stateIn,
                    parentStateDown = stateDown,
                    stateUp = stateUp,
                )
            }
        }
        if (!stateUp.isTextEmpty()) {
            resultBlocks.add(
                createBlock(
                    stateDown = stateDown,
                    stateUp = stateUp,
                    marginTop = marginTop,
                ),
            )
            stateUp.clearText()
        }
    }

    private fun interpretTextAttr(
        node: BibleTextNode,
        stateIn: StateIn,
        stateDown: StateDown,
        stateUp: StateUp,
    ) {
        if (stateDown.smallcaps) {
            stateDown.currentFont = BibleTextFontOption.SMALL_CAPS
        }

        node.classes.forEach { c ->
            when (c) {
                "wj" -> stateDown.woc = true
                "yv-v", "verse" -> {
                    node.attributes["v"]?.toIntOrNull()?.let { verseNum ->
                        stateUp.verse = verseNum
                        stateUp.rendering = (verseNum >= stateIn.fromVerse) && (verseNum <= stateIn.toVerse)
                    }
                }

                "nd", "sc" -> {
                    stateDown.currentFont = BibleTextFontOption.SMALL_CAPS
                    stateDown.smallcaps = true
                }

                "tl", "it", "add" -> stateDown.currentFont = BibleTextFontOption.TEXT_ITALIC
                "qs", "qt" -> stateDown.currentFont = BibleTextFontOption.TEXT_ITALIC
                else -> {
                    if (!listOf(
                            "yv-v",
                            "verse",
                            "yv-vlbl",
                            "vlbl",
                            "yv-n",
                            "f",
                            "fr",
                            "ft",
                            "qs",
                            "sc",
                            "nd",
                            "cl",
                            "w",
                            "litl",
                            "rq",
                            "x",
                        ).contains(c)
                    ) {
                        assertionFailed("interpretTextAttr: unexpected ", c)
                    }
                }
            }
        }
    }

    private fun interpretBlockClasses(
        classes: List<String>,
        stateIn: StateIn,
        stateDown: StateDown,
        stateUp: StateUp,
        setMarginTop: (Dp) -> Unit,
    ) {
        var newAlignment = stateDown.alignment
        var newTextCategory = stateDown.textCategory
        var newSmallCaps = stateDown.smallcaps
        var newCurrentFont = stateDown.currentFont

        val indentStep = TextUnit(stateIn.fonts.baseSize.value, TextUnitType.Sp).div(2)
        val noIndent = TextUnit(0f, TextUnitType.Sp)

        val ignoredTags =
            setOf(
                "s1", // Change line-height to 1em. Co-occurs with "yv-h".
                "b", // Poetry text stanza break (e.g. stanza break)
                "lh", // A list header (introductory remark)
                "li", // A list entry, level 1 (if single level)
                "li1", // A list entry, level 1 (if multiple levels)
                "li2", // A list entry, level 2
                "li3", // A list entry, level 3
                "li4", // A list entry, level 4
                "lf", // List footer (introductory remark)
                "mr", // handled inside yv-h
                "ms", // handled inside yv-h
                "ms1", // handled inside yv-h
                "ms2", // handled inside yv-h
                "ms3", // handled inside yv-h
                "ms4", // handled inside yv-h
                "s2", // handled inside yv-h
                "s3", // handled inside yv-h
                "s4", // handled inside yv-h
                "sp", // handled inside yv-h
                "iex", // see John 7:52
                "ms1",
                "qa",
                "r",
                "sr",
                "po",
            )

        for (c in classes) {
            when (c) {
                "p" -> { // Standard paragraph
                    stateUp.firstLineHeadIndent = indentStep * 2
                    stateUp.headIndent = noIndent
                }

                "m", "nb" -> { // No-break paragraph, flush left
                    stateUp.firstLineHeadIndent = noIndent
                    stateUp.headIndent = noIndent
                }

                "pr", "qr" -> { // Right-aligned paragraph
                    newAlignment = TextAlign.End
                }

                "pc", "qc" -> { // Centered paragraph
                    newAlignment = TextAlign.Center
                    newSmallCaps = true
                    newTextCategory = BibleTextCategory.HEADER
                }

                "mi" -> { // Indented, flush-left paragraph
                    stateUp.firstLineHeadIndent = noIndent
                    stateUp.headIndent = indentStep.times(2)
                }

                "pi", "pi1" -> { // Paragraph, indented level 1
                    stateUp.firstLineHeadIndent = indentStep
                    stateUp.headIndent = noIndent
                }

                "pi2" -> { // Paragraph, indented level 2
                    stateUp.firstLineHeadIndent = indentStep.times(2)
                    stateUp.headIndent = indentStep
                }

                "pi3" -> { // Paragraph, indented level 3
                    stateUp.firstLineHeadIndent = indentStep.times(4)
                    stateUp.headIndent = indentStep.times(3)
                }
                // Poetry and lists have their indentation reset for now
                "iq", "iq1", "q", "q1", "qm", "qm1", "li1" -> {
                    stateUp.firstLineHeadIndent = noIndent
                    stateUp.headIndent = noIndent
                }

                "iq2", "q2", "qm2", "li2" -> {
                    stateUp.firstLineHeadIndent = noIndent
                    stateUp.headIndent = noIndent
                }

                "iq3", "q3", "qm3", "li3" -> {
                    stateUp.firstLineHeadIndent = noIndent
                    stateUp.headIndent = noIndent
                }

                "iq4", "q4", "qm4", "li4" -> {
                    stateUp.firstLineHeadIndent = noIndent
                    stateUp.headIndent = noIndent
                }

                "pm", "pmo", "pmc", "pmr" -> { // Embedded text paragraph
                    stateUp.firstLineHeadIndent = noIndent
                    stateUp.headIndent = indentStep.times(2)
                }

                "d" -> { // Descriptive title (e.g., in Psalms)
                    newCurrentFont = BibleTextFontOption.HEADER_ITALIC
                    newTextCategory = BibleTextCategory.HEADER
                    if (!stateIn.renderHeadlines) {
                        stateUp.rendering = false
                    }
                }

                "yv-h", "yvh" -> { // YouVersion-specific header
                    newTextCategory = BibleTextCategory.HEADER
                    setMarginTop(stateIn.fonts.baseSize.value.dp)

                    // Determine the specific header font based on other co-occurring classes
                    newCurrentFont =
                        when {
                            classes.contains("ms") || classes.contains("ms1") -> BibleTextFontOption.HEADER2
                            classes.contains("mr") -> {
                                setMarginTop(0.dp) // Override margin for this specific header
                                BibleTextFontOption.HEADER_SMALLER
                            }

                            classes.contains("s2") || classes.contains("ms2") -> BibleTextFontOption.HEADER2
                            classes.contains("s3") || classes.contains("ms3") -> BibleTextFontOption.HEADER3
                            classes.contains("s4") || classes.contains("ms4") -> BibleTextFontOption.HEADER4
                            classes.contains(
                                "sp",
                            ) || classes.contains("r") || classes.contains("sr") -> BibleTextFontOption.HEADER_ITALIC

                            else -> BibleTextFontOption.HEADER // Default header style
                        }

                    stateUp.firstLineHeadIndent = noIndent
                    if (!stateIn.renderHeadlines) {
                        stateUp.rendering = false
                    }
                }

                else -> {
                    if (c !in ignoredTags &&
                        !c.startsWith("ms") && !c.startsWith("s") // Ignore other header variants already handled
                    ) {
                        assertionFailed("interpreting block classes: unexpected ", c)
                    }
                }
            }
        }

        stateDown.apply {
            alignment = newAlignment
            textCategory = newTextCategory
            smallcaps = newSmallCaps
            currentFont = newCurrentFont
        }
    }
}

// --- Data Classes and Helpers ---

data class BibleTextBlock(
    val id: UUID = UUID.randomUUID(),
    val text: AnnotatedString,
    val chapter: Int,
    val rows: List<List<AnnotatedString>> = emptyList(),
    val headIndent: TextUnit,
    val marginTop: Dp,
    val alignment: TextAlign,
    val footnotes: List<AnnotatedString>,
)

enum class BibleTextCategory {
    SCRIPTURE,
    VERSE_LABEL,
    FOOTNOTE_MARKER,
    FOOTNOTE_TEXT,
    HEADER,
}

/**
 * An extension function to mimic `markWithTextCategory`.
 * This adds a metadata tag to a range of text.
 */
fun AnnotatedString.Builder.addTextCategoryAnnotation(
    category: BibleTextCategory,
    start: Int,
    end: Int,
) {
    addStringAnnotation(
        tag = BibleTextCategoryAttribute.NAME,
        annotation = category.name,
        start = start,
        end = end,
    )
}

/**
 * A helper to trim trailing whitespace from an AnnotatedString.
 * Since AnnotatedString is immutable, this function returns a new one.
 */
fun AnnotatedString.trimTrailingWhitespace(): AnnotatedString {
    var endIndex = this.text.length - 1
    while (endIndex >= 0 && this.text[endIndex].isWhitespace()) {
        endIndex--
    }

    if (endIndex < this.text.length - 1) {
        return this.subSequence(0, endIndex + 1)
    }

    return this // Return the original if no whitespace was trimmed
}

// --- State Management Classes ---

data class StateIn(
    val versionId: Int,
    val bookUSFM: String,
    val currentChapter: Int,
    val fromVerse: Int,
    val toVerse: Int,
    val renderVerseNumbers: Boolean,
    val renderHeadlines: Boolean,
    val renderFootnotes: Boolean,
    val footnoteMarker: AnnotatedString?,
    val textColor: Color,
    val wocColor: Color,
    val fonts: BibleTextFonts,
)

class StateDown(
    var woc: Boolean = false,
    var smallcaps: Boolean = false,
    var alignment: TextAlign = TextAlign.Start,
    var currentFont: BibleTextFontOption,
    var textCategory: BibleTextCategory,
    var nodeDepth: Int = 0,
) {
    fun copy(): StateDown =
        StateDown(
            woc = woc,
            smallcaps = smallcaps,
            alignment = alignment,
            currentFont = currentFont,
            textCategory = textCategory,
            nodeDepth = nodeDepth,
        )
}

class StateUp(
    var rendering: Boolean,
    var firstLineHeadIndent: TextUnit = TextUnit(0f, TextUnitType.Sp),
    var headIndent: TextUnit = TextUnit(0f, TextUnitType.Sp),
    val versionId: Int,
    val bookUSFM: String,
    val chapter: Int,
    var verse: Int,
    var textBuilder: AnnotatedString.Builder = AnnotatedString.Builder(),
    val footnotes: MutableList<AnnotatedString> = mutableListOf(),
) {
    fun append(
        text: String,
        style: SpanStyle,
        category: BibleTextCategory,
    ) {
        textBuilder.withStyle(style) {
            append(text)
            // Tag with metadata for later use (e.g., click handlers)
            addStringAnnotation(
                tag = BibleTextCategoryAttribute.NAME,
                annotation = category.name,
                start = textBuilder.length - text.length,
                end = textBuilder.length,
            )
            if (verse > 0) {
                addStringAnnotation(
                    tag = BibleReferenceAttribute.NAME,
                    annotation = "$versionId:$bookUSFM:$chapter:$verse",
                    start = textBuilder.length - text.length,
                    end = textBuilder.length,
                )
            }
        }
    }

    fun appendFootnote(
        text: AnnotatedString,
        category: BibleTextCategory,
    ) {
        if (text.isNotEmpty()) {
            val annotatedString =
                buildAnnotatedString {
                    append(text)
                    addStringAnnotation(
                        tag = BibleTextCategoryAttribute.NAME,
                        annotation = category.name + ":" + footnotes.size,
                        start = 0 - text.length,
                        end = text.length,
                    )
                }
            textBuilder.append(annotatedString)
        }
    }

    fun endsWithSpace(): Boolean = textBuilder.toAnnotatedString().lastOrNull()?.isWhitespace() ?: false

    fun clearText() {
        textBuilder = AnnotatedString.Builder()
    }

    fun isTextEmpty(): Boolean = textBuilder.length == 0
}

// --- Annotation Tags for AnnotatedString ---
object BibleReferenceAttribute {
    const val NAME = "BibleReference"
}

object BibleTextCategoryAttribute {
    const val NAME = "BibleTextCategory"
}
