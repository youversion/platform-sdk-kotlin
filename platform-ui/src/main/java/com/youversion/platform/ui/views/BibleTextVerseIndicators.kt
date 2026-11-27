package com.youversion.platform.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.ui.views.rendering.BibleReferenceAttribute
import com.youversion.platform.ui.views.rendering.BibleTextCategory
import com.youversion.platform.ui.views.rendering.BibleTextCategoryAttribute
import kotlin.math.roundToInt

private const val NON_BREAKING_SPACE = "\u00A0"
private val IndicatorCornerRadius = 4.dp

// Rec. 601 luma weights and threshold, the same ones Swift uses to decide when text over a highlight turns white.
private const val RED_LUMA_WEIGHT = 0.299f
private const val GREEN_LUMA_WEIGHT = 0.587f
private const val BLUE_LUMA_WEIGHT = 0.114f
private const val DARK_LUMA_THRESHOLD = 0.5f

/** A rendered verse number that [style] marks as an indicator. [range] covers the characters its marker spans. */
internal data class VerseIndicator(
    val reference: BibleReference,
    val range: IntRange,
    val style: BibleVerseIndicatorStyle,
)

/**
 * The verse numbers in this text that [style] gives a style to, each trimmed to its digits so the marker hugs the
 * number rather than the spaces the renderer places around it. A verse that also prints an alternate number is
 * marked once, on its first number.
 */
internal fun AnnotatedString.verseIndicators(
    style: (BibleReference) -> BibleVerseIndicatorStyle?,
): List<VerseIndicator> =
    getStringAnnotations(tag = BibleTextCategoryAttribute.NAME, start = 0, end = length)
        .filter { it.item == BibleTextCategory.VERSE_LABEL.name }
        .mapNotNull { label ->
            val first = (label.start until label.end).firstOrNull { !this[it].isWhitespace() }
            val last = (label.start until label.end).lastOrNull { !this[it].isWhitespace() }
            if (first == null || last == null) return@mapNotNull null
            val reference =
                getStringAnnotations(tag = BibleReferenceAttribute.NAME, start = first, end = first + 1)
                    .firstOrNull()
                    ?.let { BibleReference.fromAnnotation(it.item) }
                    ?: return@mapNotNull null
            style(reference)?.let { VerseIndicator(reference, first..last, it) }
        }.distinctBy { it.reference }

/**
 * The text with a non-breaking space added on each side of every verse number in [numbers], as found by
 * [verseIndicators], so the marker has room around the number instead of touching the words next to it, paired with
 * those indicators. Each indicator's range spans its number and both added spaces. Annotations and styles stretch
 * over the spaces added inside them.
 */
internal fun AnnotatedString.withVerseIndicators(
    numbers: List<VerseIndicator>,
): Pair<AnnotatedString, List<VerseIndicator>> {
    if (numbers.isEmpty()) return this to emptyList()
    val insertions = numbers.flatMap { listOf(it.range.first, it.range.last + 1) }.sorted()

    fun shifted(offset: Int): Int = offset + insertions.count { it < offset }
    val padded =
        buildAnnotatedString {
            var start = 0
            insertions.forEach { offset ->
                append(text.substring(start, offset))
                append(NON_BREAKING_SPACE)
                start = offset
            }
            append(text.substring(start))
            spanStyles.forEach { addStyle(it.item, shifted(it.start), shifted(it.end)) }
            paragraphStyles.forEach { addStyle(it.item, shifted(it.start), shifted(it.end)) }
            getStringAnnotations(0, this@withVerseIndicators.length).forEach {
                addStringAnnotation(it.tag, it.item, shifted(it.start), shifted(it.end))
            }
        }
    val indicators =
        numbers.map { number ->
            val first = shifted(number.range.first)
            VerseIndicator(number.reference, first..first + number.range.last - number.range.first + 2, number.style)
        }
    return padded to indicators
}

/**
 * This indicator as painted. Outside the focused verse its marker fades to [unfocusedAlpha] and its number keeps the
 * dimmed text color around it, as in the Swift SDK. Otherwise its number is white on a dark highlight, or its style's
 * foreground color.
 */
internal fun VerseIndicator.painted(
    isInFocus: Boolean,
    unfocusedAlpha: Float,
    isOnDarkHighlight: Boolean,
): VerseIndicator {
    val alpha = if (isInFocus) 1f else unfocusedAlpha
    val foregroundColor =
        when {
            !isInFocus -> null
            isOnDarkHighlight -> Color.White
            else -> style.foregroundColor
        }
    return VerseIndicator(
        reference = reference,
        range = range,
        style =
            BibleVerseIndicatorStyle(
                backgroundColor = style.backgroundColor.copy(alpha = style.backgroundColor.alpha * alpha),
                foregroundColor = foregroundColor,
                minimumTouchTargetSize = style.minimumTouchTargetSize,
                contentDescription = style.contentDescription,
            ),
    )
}

/** Whether text over this highlight should be white; a translucent color is measured over a white page. */
internal fun Color.isDarkHighlight(): Boolean {
    fun overWhitePage(channel: Float) = alpha * channel + 1 - alpha
    val luma =
        RED_LUMA_WEIGHT * overWhitePage(red) +
            GREEN_LUMA_WEIGHT * overWhitePage(green) +
            BLUE_LUMA_WEIGHT * overWhitePage(blue)
    return luma < DARK_LUMA_THRESHOLD
}

/** The text with each indicator's number recolored to its [BibleVerseIndicatorStyle.foregroundColor], if it has one. */
internal fun AnnotatedString.withIndicatorColors(indicators: List<VerseIndicator>): AnnotatedString {
    val recolored = indicators.filter { it.style.foregroundColor != null }
    if (recolored.isEmpty()) return this
    return buildAnnotatedString {
        append(this@withIndicatorColors)
        recolored.forEach { indicator ->
            addStyle(
                SpanStyle(color = indicator.style.foregroundColor!!),
                indicator.range.first,
                indicator.range.last + 1,
            )
        }
    }
}

/**
 * Where the marker behind [indicator] is drawn: across its characters and the full height of their line, inset by
 * [inset] so markers on neighboring lines never touch.
 */
internal fun TextLayoutResult.indicatorBounds(
    indicator: VerseIndicator,
    inset: Float,
): Rect {
    val line = getLineForOffset(indicator.range.first)
    val boxes = indicator.range.map { getBoundingBox(it) }
    val top = getLineTop(line)
    val bottom = getLineBottom(line)
    val verticalInset = minOf(inset, (bottom - top) / 2)
    return Rect(
        left = boxes.minOf { it.left },
        top = top + verticalInset,
        right = boxes.maxOf { it.right },
        bottom = bottom - verticalInset,
    )
}

/** Draws each indicator's marker, meant to be called after highlights so the marker sits over them. */
internal fun DrawScope.drawVerseIndicators(
    layoutResult: TextLayoutResult,
    indicators: List<VerseIndicator>,
) {
    indicators.forEach { indicator ->
        val bounds = layoutResult.indicatorBounds(indicator = indicator, inset = 1f)
        drawRoundRect(
            color = indicator.style.backgroundColor,
            topLeft = bounds.topLeft,
            size = bounds.size,
            cornerRadius = CornerRadius(IndicatorCornerRadius.toPx()),
        )
    }
}

/**
 * Where each painted indicator's tap target sits, in root coordinates, so a tap that lands in several overlapping
 * targets reaches the verse number closest to it rather than whichever target happens to be drawn on top.
 */
internal class VerseIndicatorTargets {
    private val targets = mutableMapOf<Any, Pair<BibleReference, Rect>>()

    /** Records where the target identified by [key] for [reference] sits, replacing what was known of it. */
    fun place(
        key: Any,
        reference: BibleReference,
        boundsInRoot: Rect,
    ) {
        targets[key] = reference to boundsInRoot
    }

    /** Forgets the target identified by [key]. */
    fun remove(key: Any) {
        targets.remove(key)
    }

    /** The verse whose target holds [positionInRoot] with its center nearest to it, or [fallback] if none is known. */
    fun closestTo(
        positionInRoot: Offset,
        fallback: BibleReference,
    ): BibleReference =
        targets.values
            .filter { (_, bounds) -> bounds.contains(positionInRoot) }
            .minByOrNull { (_, bounds) -> (bounds.center - positionInRoot).getDistanceSquared() }
            ?.first ?: fallback
}

/** The tap targets of the [BibleText] being drawn, shared by its blocks so overlapping targets resolve across them. */
internal val LocalVerseIndicatorTargets = staticCompositionLocalOf<VerseIndicatorTargets?> { null }

/**
 * An invisible tap target centered on [indicator]'s marker and at least its style's minimum size. [origin] is where
 * the text's layout starts inside the box holding both, measured from that box's top left. The target may reach past
 * that box, which it never makes larger, so a paragraph shorter than the target keeps its own height. A touch is handed
 * to the verse in [targets] closest to it, so where targets overlap, each number keeps the taps nearest to it. A
 * keyboard, D-pad or accessibility activation, which has no position, opens the target's own verse, even after a
 * touch on the target was dragged away or cancelled.
 */
@Composable
internal fun VerseIndicatorTarget(
    indicator: VerseIndicator,
    layoutResult: TextLayoutResult,
    origin: Offset,
    targets: VerseIndicatorTargets,
    onTap: (BibleReference) -> Unit,
) {
    val density = LocalDensity.current
    val marker = layoutResult.indicatorBounds(indicator = indicator, inset = 1f)
    val minimumSize = with(density) { indicator.style.minimumTouchTargetSize.toSize() }
    val width = maxOf(marker.width, minimumSize.width)
    val height = maxOf(marker.height, minimumSize.height)
    val left = origin.x + marker.center.x - width / 2
    val top = origin.y + marker.center.y - height / 2
    val label =
        indicator.style.contentDescription ?: indicator.reference.verseStart
            ?.toString()
            .orEmpty()
    val key = remember { Any() }
    var positionInRoot by remember { mutableStateOf(Offset.Zero) }
    var pressPositionInRoot by remember { mutableStateOf<Offset?>(null) }
    DisposableEffect(targets, key) { onDispose { targets.remove(key) } }

    Box(
        modifier =
            Modifier
                .absoluteOffset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .wrapContentSize(align = AbsoluteAlignment.TopLeft, unbounded = true)
                .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                .testTag("verse_indicator_${indicator.reference.asUSFM}")
                .onGloballyPositioned { coordinates ->
                    positionInRoot = coordinates.positionInRoot()
                    targets.place(
                        key = key,
                        reference = indicator.reference,
                        boundsInRoot = Rect(positionInRoot, coordinates.size.toSize()),
                    )
                }.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes
                                .firstOrNull { it.changedToDown() }
                                ?.let { pressPositionInRoot = positionInRoot + it.position }
                            if (event.changes.none { it.pressed }) {
                                awaitPointerEvent(PointerEventPass.Final)
                                pressPositionInRoot = null
                            }
                        }
                    }
                }.clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                ) {
                    onTap(
                        pressPositionInRoot?.let { targets.closestTo(it, fallback = indicator.reference) }
                            ?: indicator.reference,
                    )
                    pressPositionInRoot = null
                }.semantics { contentDescription = label },
    )
}
