package com.youversion.platform.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.core.highlights.models.BibleHighlight
import com.youversion.platform.ui.R
import com.youversion.platform.ui.theme.UntitledSerif
import com.youversion.platform.ui.views.rendering.BibleReferenceAttribute
import com.youversion.platform.ui.views.rendering.BibleTextBlock

private const val FOOTNOTE_IMAGE_ID = "footnote_image_id"

private val DefaultFootnoteMarker: AnnotatedString =
    buildAnnotatedString {
        pushStyle(SpanStyle(baselineShift = BaselineShift.Superscript))
        append("\u00A0※ ")
        pop()
    }

internal val ImageFootnoteMarker: AnnotatedString =
    buildAnnotatedString { appendInlineContent(id = FOOTNOTE_IMAGE_ID) }

data class BibleTextOptions(
    val fontFamily: FontFamily = UntitledSerif,
    val fontSize: TextUnit = 16.sp,
    /**
     * Extra space between lines as a fraction of [fontSize]; null means
     * [DEFAULT_LINE_SPACING_FRACTION].
     */
    val lineSpacingFraction: Float? = null,
    val paragraphSpacing: TextUnit? = null,
    val textColor: Color? = null,
    val wocColor: Color = Color(0xFFF04C59), // YouVersion red
    val renderHeadlines: Boolean = true,
    val renderVerseNumbers: Boolean = true,
    val footnoteMode: BibleTextFootnoteMode = BibleTextFootnoteMode.NONE,
    val footnoteMarker: AnnotatedString? = DefaultFootnoteMarker,
    val selectionColor: Color? = null,
) {
    internal val inlineContentMap =
        mapOf(
            FOOTNOTE_IMAGE_ID to
                InlineTextContent(
                    Placeholder(
                        width = fontSize * 1.5,
                        height = fontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    val iconSize = with(LocalDensity.current) { fontSize.toDp() }
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .offset(y = -iconSize / 4)
                                .alpha(0.8f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_material_footnotes),
                            contentDescription = stringResource(R.string.footnote_content_desc),
                            modifier = Modifier.size(iconSize),
                            tint = LocalContentColor.current.copy(alpha = 0.6f),
                        )
                    }
                },
        )

    /** Line height including [lineSpacingFraction], or the default when it is unset. */
    internal val resolvedLineHeight: TextUnit
        get() = fontSize * (BASE_LINE_HEIGHT + (lineSpacingFraction ?: DEFAULT_LINE_SPACING_FRACTION))

    /** Extra leading in the same units as [fontSize], used to pad a block's bottom margin. */
    internal val extraLeading: Float
        get() = fontSize.value * (lineSpacingFraction ?: DEFAULT_LINE_SPACING_FRACTION)

    companion object {
        /** Extra leading applied when [lineSpacingFraction] is unset. */
        const val DEFAULT_LINE_SPACING_FRACTION: Float = 0.4f

        // The line height a font already carries before any extra leading is added.
        private const val BASE_LINE_HEIGHT = 1.2f
    }
}

@PlatformInternalApi
fun Int.convertToEnumeration(): String {
    val value = 'a'.code + minOf(25, this)
    return value.toChar().toString()
}

enum class BibleTextFootnoteMode {
    NONE,
    INLINE,
    MARKER,
    LETTERS,
    IMAGE,
}

enum class BibleTextLoadingPhase {
    INACTIVE,
    LOADING,
    FAILED,
    NOT_PERMITTED,
    SUCCESS,
}

/**
 * Renders the chapter, or the passage within it, named by [reference].
 *
 * @param reference The passage to render.
 * @param textOptions Text styling options (font family, font size, line spacing, etc.).
 * @param selectedVerses The verses to draw as selected.
 * @param onVerseSelectedChange Callback invoked when a tap adds a verse to or removes it from the selection.
 * @param onVerseTap Callback invoked when a verse is tapped, with the tap position.
 * @param onFootnoteTap Callback invoked when a footnote icon is tapped, providing the footnotes for that verse.
 * @param placeholder A composable to display during loading and error states.
 * @param onStateChange Callback invoked when the loading phase changes.
 * @param onBlocksChange Callback invoked with the rendered blocks each time a load succeeds, so a host that needs
 * the same content can reuse them instead of rendering the chapter twice. It fires once per successful load: on
 * first composition, and again whenever [reference] or [textOptions] compares unequal to the previous value. It
 * does not fire when a load fails, is not permitted, or is cancelled. The blocks are rebuilt from scratch on every
 * load and carry no stable identity, so replace the previous list wholesale rather than diffing against it.
 */
@Composable
fun BibleText(
    reference: BibleReference,
    textOptions: BibleTextOptions = BibleTextOptions(),
    selectedVerses: Set<BibleReference> = emptySet(),
    onVerseSelectedChange: (Set<BibleReference>) -> Unit = {},
    onVerseTap: ((reference: BibleReference, position: Offset) -> Unit)? = null,
    onFootnoteTap: ((reference: BibleReference, footNotes: List<AnnotatedString>) -> Unit)? = null,
    placeholder: @Composable (BibleTextLoadingPhase) -> Unit = { StandardPlaceholder(it) },
    onStateChange: (BibleTextLoadingPhase) -> Unit = {},
    onBlocksChange: (List<BibleTextBlock>) -> Unit = {},
) {
    val blocksState = rememberBibleTextBlocksState(reference = reference, textOptions = textOptions)

    LaunchedEffect(blocksState.loadingPhase) {
        onStateChange(blocksState.loadingPhase)
    }

    LaunchedEffect(blocksState.blocks) {
        if (blocksState.loadingPhase == BibleTextLoadingPhase.SUCCESS) {
            onBlocksChange(blocksState.blocks)
        }
    }

    if (blocksState.loadingPhase != BibleTextLoadingPhase.SUCCESS) {
        placeholder(blocksState.loadingPhase)
    } else {
        Column(horizontalAlignment = blocksState.horizontalAlignment) {
            blocksState.visibleBlocks.indices.forEach { index ->
                BibleBlockContent(
                    visibleBlocks = blocksState.visibleBlocks,
                    index = index,
                    textOptions = textOptions,
                    selectedVerses = selectedVerses,
                    highlights = blocksState.highlights,
                    onVerseTap = onVerseTap,
                    onFootnoteTap = onFootnoteTap,
                )
            }
        }
    }
}

internal fun BibleReference.Companion.fromAnnotation(annotation: String): BibleReference {
    val split = annotation.split(":")
    return BibleReference(
        versionId = split[0].toInt(),
        bookUSFM = split[1],
        chapter = split[2].toInt(),
        verse = split[3].toInt(),
    )
}

/**
 * Returns one merged character range per verse [isIncluded] accepts, spanning from the first annotation start
 * to the last annotation end for that verse.
 */
private fun AnnotatedString.mergedVerseRanges(isIncluded: (BibleReference) -> Boolean): List<IntRange> =
    getStringAnnotations(
        tag = BibleReferenceAttribute.NAME,
        start = 0,
        end = length,
    ).filter { annotation ->
        isIncluded(BibleReference.fromAnnotation(annotation.item))
    }.groupBy { it.item }
        .map { (_, annotations) ->
            annotations.first().start until annotations.last().end
        }

/** Returns one merged character range per selected verse. */
internal fun AnnotatedString.selectedCharacterRanges(selectedVerses: Set<BibleReference>): List<IntRange> {
    if (selectedVerses.isEmpty()) return emptyList()

    return mergedVerseRanges { reference -> selectedVerses.any { it.overlaps(reference) } }
}

/** Returns one merged character range per verse [focusedReference] covers. */
internal fun AnnotatedString.focusedCharacterRanges(focusedReference: BibleReference): List<IntRange> =
    mergedVerseRanges { reference -> focusedReference.contains(reference) }

/**
 * The text with everything outside [focusedRanges] repainted in [dimmedColor], so the verse in focus is told
 * apart from the ones it shares a block with rather than the block standing out whole.
 */
internal fun AnnotatedString.dimmedOutside(
    focusedRanges: List<IntRange>,
    dimmedColor: Color,
): AnnotatedString {
    val dimmedStyle = SpanStyle(color = dimmedColor)

    return buildAnnotatedString {
        append(this@dimmedOutside)

        var start = 0
        focusedRanges.sortedBy { it.first }.forEach { range ->
            if (range.first > start) addStyle(dimmedStyle, start, range.first)
            start = maxOf(start, range.last + 1)
        }
        if (start < length) addStyle(dimmedStyle, start, length)
    }
}

/**
 * Resolves the cached highlights overlapping [reference] into the colors to draw behind the text,
 * applying [highlightAlpha] so that a dark reader theme dims them instead of letting them overwhelm
 * the text they sit behind.
 *
 * Highlights whose stored color is not valid hex are dropped.
 */
internal fun highlightColorsForReference(
    cachedHighlights: List<BibleHighlight>,
    reference: BibleReference,
    highlightAlpha: Float,
): Map<BibleReference, Color> =
    cachedHighlights
        .asSequence()
        .filter { it.bibleReference.overlaps(reference) }
        .mapNotNull { cached ->
            cached.hexColor.toHighlightColorOrNull()?.let { color ->
                cached.bibleReference to color.copy(alpha = highlightAlpha)
            }
        }.toMap()

/**
 * Returns one merged character range per highlighted verse, paired with its highlight color,
 * spanning from the first annotation start to the last annotation end for that verse.
 *
 * When a verse matches more than one highlight, the first overlapping entry in [highlights] wins.
 */
internal fun AnnotatedString.highlightedCharacterRanges(
    highlights: Map<BibleReference, Color>,
): List<Pair<IntRange, Color>> {
    if (highlights.isEmpty()) return emptyList()

    return getStringAnnotations(
        tag = BibleReferenceAttribute.NAME,
        start = 0,
        end = length,
    ).mapNotNull { annotation ->
        val reference = BibleReference.fromAnnotation(annotation.item)
        val color = highlights.entries.firstOrNull { it.key.overlaps(reference) }?.value
        color?.let { annotation to it }
    }.groupBy { (annotation, _) -> annotation.item }
        .map { (_, entries) ->
            val annotations = entries.map { it.first }
            val range = annotations.first().start until annotations.last().end
            range to entries.first().second
        }
}

/**
 * Computes the horizontal `[left, right]` span of a highlight band on a single line, honoring text
 * direction.
 *
 * On the line where the highlight starts or ends, the corresponding boundary is the caret position
 * ([startCaretX] / [endCaretX]); on lines the highlight only passes through, that boundary extends to
 * the line's leading or trailing text edge ([lineLeft] / [lineRight]) so a wrapped highlight reads as
 * one continuous band that stops at the text rather than filling the container width. Taking the
 * min/max keeps the span valid for right-to-left text, where the start caret sits to the right of the
 * end caret.
 */
internal fun highlightLineSpan(
    isRtl: Boolean,
    isStartLine: Boolean,
    isEndLine: Boolean,
    startCaretX: Float,
    endCaretX: Float,
    lineLeft: Float,
    lineRight: Float,
): Pair<Float, Float> {
    val leadingEdge = if (isRtl) lineRight else lineLeft
    val trailingEdge = if (isRtl) lineLeft else lineRight
    val startEdge = if (isStartLine) startCaretX else leadingEdge
    val endEdge = if (isEndLine) endCaretX else trailingEdge
    return minOf(startEdge, endEdge) to maxOf(startEdge, endEdge)
}

/**
 * Parses a `#RRGGBB` or `#AARRGGBB` hex string (as stored on [com.youversion.platform.core.highlights.models.BibleHighlight.hexColor])
 * into a Compose [Color], or returns null when the string is not valid hex. Six-digit values are treated as fully opaque.
 */
internal fun String.toHighlightColorOrNull(): Color? {
    val hex = removePrefix("#")
    val value = hex.toLongOrNull(radix = 16) ?: return null
    return when (hex.length) {
        6 -> Color(0xFF000000L or value)
        8 -> Color(value)
        else -> null
    }
}

/**
 * Draws a continuous filled background rectangle behind every text line that contains characters in
 * [highlightedRanges]. Interior wrapped lines span the full width of their text so the highlight appears
 * as a single unbroken color rather than per-character backgrounds that would look jagged at line breaks.
 */
private fun DrawScope.drawHighlightBackgrounds(
    layoutResult: TextLayoutResult,
    highlightedRanges: List<Pair<IntRange, Color>>,
) {
    highlightedRanges
        .filterNot { (range, _) -> range.isEmpty() }
        .forEach { (range, color) ->
            val startLine = layoutResult.getLineForOffset(range.first)
            val endLine = layoutResult.getLineForOffset(range.last)
            val startCaretX = layoutResult.getHorizontalPosition(range.first, true)
            val lastCharBounds = layoutResult.getBoundingBox(range.last)

            (startLine..endLine).forEach { line ->
                val lineTop = layoutResult.getLineTop(line)
                val lineBottom = layoutResult.getLineBottom(line)
                val isRtl =
                    layoutResult.getParagraphDirection(layoutResult.getLineStart(line)) ==
                        ResolvedTextDirection.Rtl
                val (lineLeft, lineRight) =
                    highlightLineSpan(
                        isRtl = isRtl,
                        isStartLine = line == startLine,
                        isEndLine = line == endLine,
                        startCaretX = startCaretX,
                        endCaretX = if (isRtl) lastCharBounds.left else lastCharBounds.right,
                        lineLeft = layoutResult.getLineLeft(line),
                        lineRight = layoutResult.getLineRight(line),
                    )
                drawRect(
                    color = color,
                    topLeft = Offset(lineLeft, lineTop),
                    size = Size(lineRight - lineLeft, lineBottom - lineTop),
                )
            }
        }
}

/**
 * Draws underlines at the bottom of each text line that contains characters in [selectedRanges].
 */
private fun DrawScope.drawSelectionUnderlines(
    layoutResult: TextLayoutResult,
    selectedRanges: List<IntRange>,
    color: Color,
    strokeWidth: Dp,
) {
    val textLength = layoutResult.layoutInput.text.length
    selectedRanges
        .filterNot { it.isEmpty() }
        .forEach { range ->
            val endOffset = (range.last + 1).coerceAtMost(textLength)
            val startLine = layoutResult.getLineForOffset(range.first)
            val endLine = layoutResult.getLineForOffset(endOffset)

            (startLine..endLine).forEach { line ->
                val lineBottom = layoutResult.getLineBottom(line)
                val lineLeft =
                    if (line == startLine) {
                        layoutResult.getHorizontalPosition(range.first, true)
                    } else {
                        layoutResult.getLineLeft(line)
                    }
                val lineRight =
                    if (line == endLine) {
                        layoutResult.getHorizontalPosition(endOffset, true)
                    } else {
                        layoutResult.getLineRight(line)
                    }
                drawLine(
                    color = color,
                    start = Offset(lineLeft, lineBottom),
                    end = Offset(lineRight, lineBottom),
                    strokeWidth = strokeWidth.toPx(),
                )
            }
        }
}

/**
 * Draws one block of Bible text, with its own selection, highlight and footnote handling.
 *
 * [isFirstBlock] and [previousMarginBottom] place the block against the one before it, so a caller emitting
 * blocks one at a time has to supply them from its own position in the chapter.
 *
 * A block holds a whole paragraph, which is often more than one verse, so a non-null [focusedReference] draws
 * the verses outside it at [unfocusedAlpha] rather than leaving the paragraph lit up whole.
 */
@Composable
internal fun BibleTextBlock(
    block: BibleTextBlock,
    textOptions: BibleTextOptions,
    isFirstBlock: Boolean,
    previousMarginBottom: Dp,
    selectedVerses: Set<BibleReference>,
    highlights: Map<BibleReference, Color>,
    focusedReference: BibleReference? = null,
    unfocusedAlpha: Float = 1f,
    onClick: (position: Offset, layoutResult: TextLayoutResult) -> Unit,
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    // Adjacent blocks share whichever margin is bigger instead of stacking both, mirroring
    // how CSS collapses margins; the topmost block gets no top margin at all.
    val marginTop = if (isFirstBlock) 0.dp else maxOf(0.dp, block.marginTop - previousMarginBottom)
    val marginBottom =
        block.marginBottom + textOptions.extraLeading.dp + (textOptions.paragraphSpacing ?: 0.sp).value.dp

    val selectionColor = textOptions.selectionColor ?: LocalContentColor.current

    val selectedRanges =
        remember(block.text, selectedVerses) {
            block.text.selectedCharacterRanges(selectedVerses)
        }

    val textColor = textOptions.textColor ?: LocalContentColor.current

    val focusedRanges =
        remember(block.text, focusedReference) {
            focusedReference?.let { block.text.focusedCharacterRanges(it) }.orEmpty()
        }

    val text =
        remember(block.text, focusedReference, focusedRanges, unfocusedAlpha, textColor) {
            if (focusedReference == null) {
                block.text
            } else {
                block.text.dimmedOutside(focusedRanges, textColor.copy(alpha = textColor.alpha * unfocusedAlpha))
            }
        }

    val highlightedRanges =
        remember(block.text, highlights, focusedReference, focusedRanges, unfocusedAlpha) {
            block.text.highlightedCharacterRanges(highlights).map { (range, color) ->
                val isInFocus =
                    focusedReference == null ||
                        focusedRanges.any { it.first <= range.first && it.last >= range.last }
                range to if (isInFocus) color else color.copy(alpha = color.alpha * unfocusedAlpha)
            }
        }

    Text(
        text = text,
        textAlign = block.alignment,
        lineHeight = textOptions.resolvedLineHeight,
        color = textOptions.textColor ?: Color.Unspecified,
        style =
            LocalTextStyle.current.copy(
                // Swift fakes this with repeated non-breaking spaces (3 per unit, capped at 24)
                // because AttributedString has no first-line indent; Compose has the real thing,
                // sized to match at a nbsp's typical width of 0.25em.
                textIndent =
                    TextIndent(
                        firstLine =
                            textOptions.fontSize * 0.25f *
                                (block.firstLineHeadIndent * 3).coerceIn(0, 24),
                    ),
            ),
        modifier =
            Modifier
                .padding(
                    start = (8 * block.headIndent).dp,
                    top = marginTop,
                    bottom = marginBottom,
                ).fillMaxWidth()
                .drawWithContent {
                    textLayoutResult?.let { drawHighlightBackgrounds(it, highlightedRanges) }
                    drawContent()
                    textLayoutResult?.let { drawSelectionUnderlines(it, selectedRanges, selectionColor, 1.dp) }
                }.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { position ->
                            textLayoutResult?.let { layoutResult ->
                                onClick(position, layoutResult)
                            }
                        },
                    )
                },
        onTextLayout = { result ->
            textLayoutResult = result
        },
        inlineContent = textOptions.inlineContentMap,
    )
}

/** Draws one block of Bible text laid out as a table, for the blocks that carry rows rather than a single run. */
@Composable
internal fun BibleTableBlock(
    block: BibleTextBlock,
    textOptions: BibleTextOptions,
    selectedVerses: Set<BibleReference>,
    highlights: Map<BibleReference, Color>,
    focusedReference: BibleReference? = null,
    unfocusedAlpha: Float = 1f,
    onVerseTap: ((reference: BibleReference, position: Offset) -> Unit)?,
) {
    val selectionColor = textOptions.selectionColor ?: LocalContentColor.current

    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val numCols = block.rows.maxOfOrNull { it.size } ?: 0
        block.rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                for (i in 0 until numCols) {
                    val cellText = row.getOrNull(i) ?: AnnotatedString("")
                    val selectedRanges =
                        remember(cellText, selectedVerses) {
                            cellText.selectedCharacterRanges(selectedVerses)
                        }
                    var cellLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    val underlineModifier =
                        Modifier.drawWithContent {
                            drawContent()
                            val layoutResult = cellLayoutResult ?: return@drawWithContent
                            drawSelectionUnderlines(layoutResult, selectedRanges, selectionColor, 2.dp)
                        }
                    if (i == 0) {
                        Box(modifier = Modifier.weight(1f)) {
                            BibleTableCell(
                                cellText = cellText,
                                textOptions = textOptions,
                                selectedVerses = selectedVerses,
                                highlights = highlights,
                                focusedReference = focusedReference,
                                unfocusedAlpha = unfocusedAlpha,
                                onVerseTap = onVerseTap,
                            )
                        }
                    } else {
                        Box {
                            BibleTableCell(
                                cellText = cellText,
                                textOptions = textOptions,
                                selectedVerses = selectedVerses,
                                highlights = highlights,
                                focusedReference = focusedReference,
                                unfocusedAlpha = unfocusedAlpha,
                                onVerseTap = onVerseTap,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BibleTableCell(
    cellText: AnnotatedString,
    textOptions: BibleTextOptions,
    selectedVerses: Set<BibleReference>,
    highlights: Map<BibleReference, Color>,
    focusedReference: BibleReference? = null,
    unfocusedAlpha: Float = 1f,
    onVerseTap: ((reference: BibleReference, position: Offset) -> Unit)?,
) {
    var cellLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val selectionColor = textOptions.selectionColor ?: LocalContentColor.current
    val textColor = textOptions.textColor ?: LocalContentColor.current
    val selectedRanges =
        remember(cellText, selectedVerses) {
            cellText.selectedCharacterRanges(selectedVerses)
        }
    val highlightedRanges =
        remember(cellText, highlights) {
            cellText.highlightedCharacterRanges(highlights)
        }
    val text =
        remember(cellText, focusedReference, unfocusedAlpha, textColor) {
            if (focusedReference == null) {
                cellText
            } else {
                cellText.dimmedOutside(
                    focusedRanges = cellText.focusedCharacterRanges(focusedReference),
                    dimmedColor = textColor.copy(alpha = textColor.alpha * unfocusedAlpha),
                )
            }
        }

    Text(
        text = text,
        lineHeight = textOptions.resolvedLineHeight,
        color = textOptions.textColor ?: Color.Unspecified,
        modifier =
            Modifier
                .drawWithContent {
                    cellLayoutResult?.let { drawHighlightBackgrounds(it, highlightedRanges) }
                    drawContent()
                    cellLayoutResult?.let { drawSelectionUnderlines(it, selectedRanges, selectionColor, 1.dp) }
                }.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { position ->
                            cellLayoutResult?.let { layoutResult ->
                                val characterIndex = layoutResult.getOffsetForPosition(position)
                                val tappedRef =
                                    cellText
                                        .getStringAnnotations(
                                            tag = BibleReferenceAttribute.NAME,
                                            start = characterIndex,
                                            end = characterIndex,
                                        ).firstOrNull()
                                        ?.item
                                        ?.let { BibleReference.fromAnnotation(it) }

                                if (tappedRef != null) {
                                    onVerseTap?.invoke(tappedRef, position)
                                }
                            }
                        },
                    )
                },
        onTextLayout = { cellLayoutResult = it },
    )
}

@Composable
internal fun StandardPlaceholder(phase: BibleTextLoadingPhase) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            BibleTextLoadingPhase.INACTIVE -> {}

            BibleTextLoadingPhase.LOADING -> {
                CircularProgressIndicator()
            }

            BibleTextLoadingPhase.NOT_PERMITTED -> {
                PlaceholderMessage(
                    icon = ImageVector.vectorResource(R.drawable.ic_material_lock),
                    iconContentDescription = stringResource(R.string.placeholder_version_unavailable_icon_content_desc),
                    message = stringResource(R.string.placeholder_version_unavailable),
                )
            }

            BibleTextLoadingPhase.FAILED -> {
                PlaceholderMessage(
                    icon = ImageVector.vectorResource(R.drawable.ic_wifi_exclamation),
                    iconContentDescription = stringResource(R.string.placeholder_offline_icon_content_desc),
                    message = stringResource(R.string.placeholder_offline),
                )
            }

            BibleTextLoadingPhase.SUCCESS -> {}
        }
    }
}

@Composable
private fun PlaceholderMessage(
    icon: ImageVector,
    iconContentDescription: String,
    message: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                ).padding(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconContentDescription,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
