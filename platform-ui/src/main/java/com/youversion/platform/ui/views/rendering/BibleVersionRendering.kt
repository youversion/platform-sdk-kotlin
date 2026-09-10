package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleTextNode
import com.youversion.platform.core.bibles.domain.BibleTextNodeType
import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.core.utilities.exceptions.BibleVersionApiException
import com.youversion.platform.ui.views.BibleTextFontOption
import com.youversion.platform.ui.views.BibleTextFonts
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import com.youversion.platform.ui.views.ImageFootnoteMarker
import com.youversion.platform.ui.views.convertToEnumeration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val DEBUG_RENDERING = false

/** Upper bound for the verse range to lay out when rendering should run to the end of the chapter. */
private const val LAST_RENDERABLE_VERSE = 999

/**
 * Provides functionality for rendering Bible references into plain text or rich text blocks
 * for use in Jetpack Compose.
 */
@PlatformInternalApi
object BibleVersionRendering {
    /**
     * Returns plain text for a Bible reference.
     */
    suspend fun plainTextOf(
        bibleChapterRepository: BibleChapterRepository,
        reference: BibleReference,
    ): String? {
        val fonts = BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 16.sp)
        return try {
            val blocks =
                textBlocks(
                    bibleChapterRepository = bibleChapterRepository,
                    reference = reference,
                    renderVerseNumbers = false,
                    renderHeadlines = false,
                    footnoteMode = BibleTextFootnoteMode.NONE,
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
     * Formats intro chapter HTML into styled AnnotatedString blocks for Compose.
     *
     * Unlike [textBlocks], this method takes raw HTML content directly and renders
     * all content without verse filtering or verse number rendering.
     */
    suspend fun introTextBlocks(
        htmlContent: String,
        versionId: Int,
        bookUSFM: String,
        renderHeadlines: Boolean = true,
        footnoteMode: BibleTextFootnoteMode,
        footnoteMarker: AnnotatedString? = null,
        textColor: Color = Color.Unspecified,
        wocColor: Color = Color.Red,
        fonts: BibleTextFonts,
    ): List<BibleTextBlock>? =
        withContext(Dispatchers.IO) {
            val rootNode: BibleTextNode? =
                try {
                    BibleTextNode.parse(htmlContent)
                } catch (_: Exception) {
                    return@withContext null
                }

            if (rootNode?.children?.isEmpty() == true) {
                return@withContext null
            }

            val stateIn =
                StateIn(
                    versionId = versionId,
                    bookUSFM = bookUSFM,
                    currentChapter = 0,
                    fromVerse = 1,
                    toVerse = LAST_RENDERABLE_VERSE,
                    renderVerseNumbers = false,
                    renderHeadlines = renderHeadlines,
                    footnoteMode = footnoteMode,
                    footnoteMarker = footnoteMarker,
                    textColor = textColor,
                    wocColor = wocColor,
                    fonts = fonts,
                )

            val resultBlocks = mutableListOf<BibleTextBlock>()
            val stateDown =
                StateDown(
                    currentFont = BibleTextFontOption.FONT_100EM,
                    textCategory = BibleTextCategory.SCRIPTURE,
                )
            val stateUp =
                StateUp(
                    rendering = true,
                    versionId = versionId,
                    bookUSFM = bookUSFM,
                    chapter = 0,
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

    /**
     * Formats Bible data into styled AnnotatedString blocks for Compose.
     */
    suspend fun textBlocks(
        bibleChapterRepository: BibleChapterRepository,
        reference: BibleReference,
        renderVerseNumbers: Boolean = true,
        renderHeadlines: Boolean = true,
        footnoteMode: BibleTextFootnoteMode,
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
                    var data = bibleChapterRepository.chapter(reference = chapterRef)
                    val node = BibleTextNode.parse(data)
                    if (node?.children?.count() == 0) {
                        bibleChapterRepository.removeVersionChapters(reference.versionId)
                        data = bibleChapterRepository.chapter(reference = chapterRef)
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
                    toVerse = reference.verseEnd ?: LAST_RENDERABLE_VERSE,
                    renderVerseNumbers = renderVerseNumbers,
                    renderHeadlines = renderHeadlines,
                    footnoteMode = footnoteMode,
                    footnoteMarker = footnoteMarker,
                    textColor = textColor,
                    wocColor = wocColor,
                    fonts = fonts,
                )

            val resultBlocks = mutableListOf<BibleTextBlock>()
            val stateDown =
                StateDown(
                    currentFont = BibleTextFontOption.FONT_100EM,
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

    private fun createBlock(
        stateDown: StateDown,
        stateUp: StateUp,
    ): BibleTextBlock {
        val block =
            BibleTextBlock(
                text = stateUp.textBuilder.toAnnotatedString(),
                chapter = stateUp.chapter,
                firstLineHeadIndent = stateUp.firstLineHeadIndent,
                headIndent = stateUp.headIndent,
                marginTop = stateDown.marginTop,
                marginBottom = stateDown.marginBottom,
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
                stateIn.fonts
                    .styleFor(stateDown.currentFont, inSmallcaps = stateDown.smallcaps)
                    .let {
                        if (stateDown.woc) it.copy(color = stateIn.wocColor) else it
                    }.let { style ->
                        stateDown.baselineShift?.let { style.copy(baselineShift = it) } ?: style
                    }
            stateUp.append(text, style, stateDown.textCategory)
        }

        if (node.classes.contains("yv-vlbl") || node.classes.contains("vlbl")) {
            if (stateUp.rendering && stateIn.renderVerseNumbers && node.children.isNotEmpty()) {
                val text = node.children.first().text
                val maybeSpace = if (stateUp.isTextEmpty() || stateUp.endsWithSpace()) "" else " "
                val verseNumText = "$maybeSpace$text\u00A0" // non-breaking space
                val verseNumStyle =
                    stateIn.fonts.styleFor(BibleTextFontOption.VERSE_NUM_FONT).copy(
                        baselineShift = stateIn.fonts.verseNumBaselineShift,
                        color = stateIn.textColor.copy(alpha = stateIn.textColor.alpha * stateIn.fonts.verseNumOpacity),
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
        if (stateIn.footnoteMode == BibleTextFootnoteMode.NONE) {
            val style = stateIn.fonts.styleFor(parentStateDown.currentFont)
            stateUp.append(text = " ", style = style, category = parentStateDown.textCategory)
            return
        }

        var stateDown =
            parentStateDown.copy().apply {
                nodeDepth = parentStateDown.nodeDepth + 1
                textCategory = BibleTextCategory.FOOTNOTE_TEXT
            }

        val marker =
            when (stateIn.footnoteMode) {
                BibleTextFootnoteMode.IMAGE -> ImageFootnoteMarker
                BibleTextFootnoteMode.LETTERS -> {
                    val style =
                        stateIn.fonts.styleFor(BibleTextFontOption.FOOTNOTE).copy(
                            baselineShift = stateIn.fonts.verseNumBaselineShift,
                            color =
                                stateIn.textColor.copy(
                                    alpha = stateIn.textColor.alpha * stateIn.fonts.verseNumOpacity,
                                ),
                        )
                    buildAnnotatedString {
                        withStyle(style) {
                            append(stateUp.nextFootnoteMarker())
                        }
                    }
                }

                else -> stateIn.footnoteMarker
            }

        if (stateIn.footnoteMode != BibleTextFootnoteMode.INLINE && marker != null) {
            val category =
                when (stateIn.footnoteMode) {
                    BibleTextFootnoteMode.IMAGE -> BibleTextCategory.FOOTNOTE_IMAGE
                    else -> BibleTextCategory.FOOTNOTE_MARKER
                }
            stateUp.appendFootnote(marker, category)

            val footState =
                StateUp(
                    rendering = true,
                    versionId = stateUp.versionId,
                    bookUSFM = stateUp.bookUSFM,
                    chapter = stateUp.chapter,
                    verse = stateUp.verse,
                )

            for (child in node.children) {
                handleBlockChild(
                    node = child,
                    stateIn = stateIn,
                    parentStateDown = stateDown,
                    stateUp = footState,
                )
            }

            if (!footState.isTextEmpty()) {
                stateUp.footnotes.add(footState.textBuilder.toAnnotatedString())
            }
        } else {
            stateDown =
                stateDown.copy().apply {
                    currentFont = BibleTextFontOption.FOOTNOTE
                }

            val defaultStyle = stateIn.fonts.styleFor(BibleTextFontOption.FONT_100EM)
            stateUp.append("[", defaultStyle, BibleTextCategory.SCRIPTURE)

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
                    firstLineHeadIndent = 0,
                    headIndent = 0,
                    marginTop = 10.dp, // A default margin for tables
                    marginBottom = 0.dp,
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
                currentFont = BibleTextFontOption.FONT_100EM // Reset font to default for table cells
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
        val stateDown =
            parentStateDown.copy().apply {
                nodeDepth = parentStateDown.nodeDepth + 1
                currentFont = BibleTextFontOption.FONT_100EM
            }

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
        )

        for ((index, child) in node.children.withIndex()) {
            if (child.type == BibleTextNodeType.BLOCK || child.type == BibleTextNodeType.TABLE) {
                if (!stateUp.isTextEmpty()) {
                    if (stateUp.rendering) {
                        resultBlocks.add(createBlock(stateDown, stateUp))
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
                    val isHeader =
                        child.classes.contains("yv-h") || child.classes.contains("yvh")
                    val savedRendering = stateUp.rendering

                    if (isHeader && stateIn.renderHeadlines) {
                        val nextVerse =
                            node.children
                                .drop(index + 1)
                                .firstNotNullOfOrNull { sibling ->
                                    firstVerseInNode(sibling)
                                }
                        val isNextVerseInRange =
                            nextVerse != null &&
                                nextVerse >= stateIn.fromVerse &&
                                nextVerse <= stateIn.toVerse

                        if (!stateUp.rendering && isNextVerseInRange) {
                            stateUp.rendering = true
                        } else if (stateUp.rendering && nextVerse != null && !isNextVerseInRange) {
                            stateUp.rendering = false
                        }
                    }

                    handleNodeBlock(
                        node = child,
                        stateIn = stateIn,
                        parentStateDown = stateDown,
                        stateUp = stateUp,
                        resultBlocks = resultBlocks,
                    )

                    if (isHeader) {
                        stateUp.rendering = savedRendering
                    }
                }
            } else if (child.type == BibleTextNodeType.SPAN && child.classes.contains("qs")) {
                if (!stateUp.isTextEmpty()) {
                    if (stateUp.rendering) {
                        resultBlocks.add(createBlock(stateDown, stateUp))
                        stateUp.clearText()
                        handleBlockChild(child, stateIn, stateDown, stateUp)
                        val selahStateDown = stateDown.copy().apply { alignment = TextAlign.End }
                        resultBlocks.add(createBlock(selahStateDown, stateUp))
                    }
                    stateUp.clearText()
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
                ),
            )
            stateUp.clearText()
        }
    }

    /**
     * Finds the first verse number in a node's subtree by searching for verse-labeled spans.
     */
    private fun firstVerseInNode(node: BibleTextNode): Int? {
        if (node.classes.contains("yv-v") || node.classes.contains("verse")) {
            node.attributes["v"]?.toIntOrNull()?.let { return it }
        }
        for (child in node.children) {
            firstVerseInNode(child)?.let { return it }
        }
        return null
    }
}

internal fun assertionFailed(
    message: String,
    detail: Any? = null,
) {
    if (!DEBUG_RENDERING) return
    println("ASSERTION FAILED: $message${detail?.toString() ?: ""}")
}

// --- State Management Classes ---

internal data class StateIn(
    val versionId: Int,
    val bookUSFM: String,
    val currentChapter: Int,
    val fromVerse: Int,
    val toVerse: Int,
    val renderVerseNumbers: Boolean,
    val renderHeadlines: Boolean,
    val footnoteMode: BibleTextFootnoteMode,
    val footnoteMarker: AnnotatedString?,
    val textColor: Color,
    val wocColor: Color,
    val fonts: BibleTextFonts,
)

internal class StateDown(
    var woc: Boolean = false,
    var smallcaps: Boolean = false,
    var alignment: TextAlign = TextAlign.Start,
    var currentFont: BibleTextFontOption,
    var baselineShift: BaselineShift? = null,
    var textCategory: BibleTextCategory,
    var nodeDepth: Int = 0,
    var marginTop: Dp = 0.dp,
    var marginBottom: Dp = 0.dp,
) {
    fun copy(): StateDown =
        StateDown(
            woc = woc,
            smallcaps = smallcaps,
            alignment = alignment,
            currentFont = currentFont,
            baselineShift = baselineShift,
            textCategory = textCategory,
            nodeDepth = nodeDepth,
            marginTop = marginTop,
            marginBottom = marginBottom,
        )
}

internal class StateUp(
    var rendering: Boolean,
    var firstLineHeadIndent: Int = 0,
    var headIndent: Int = 0,
    val versionId: Int,
    val bookUSFM: String,
    val chapter: Int,
    var verse: Int,
    var textBuilder: AnnotatedString.Builder = AnnotatedString.Builder(),
    val footnotes: MutableList<AnnotatedString> = mutableListOf(),
    private var footnoteCount: Int = 0,
    private var footnoteVerseTracker: Int = 0,
) {
    fun nextFootnoteMarker(): String {
        if (verse != footnoteVerseTracker) {
            footnoteCount = 0
            footnoteVerseTracker = verse
        }
        val marker = footnoteCount.convertToEnumeration()
        footnoteCount++
        return "\u00A0$marker "
    }

    fun append(
        text: String,
        style: SpanStyle,
        category: BibleTextCategory,
    ) {
        textBuilder.withStyle(style) {
            val start = textBuilder.length
            append(text)
            val end = textBuilder.length
            addTextCategoryAnnotation(
                category = category,
                start = start,
                end = end,
            )
            if (verse > 0 &&
                category != BibleTextCategory.HEADER &&
                category != BibleTextCategory.BOOK_TITLE
            ) {
                addStringAnnotation(
                    tag = BibleReferenceAttribute.NAME,
                    annotation = "$versionId:$bookUSFM:$chapter:$verse",
                    start = start,
                    end = end,
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
                        tag = BibleReferenceAttribute.NAME,
                        annotation = "$versionId:$bookUSFM:$chapter:$verse",
                        start = 0,
                        end = text.length + 1,
                    )
                    addTextCategoryAnnotation(
                        category = category,
                        start = 0,
                        end = text.length + 1,
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
