package com.youversion.platform.ui.views

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.ui.views.rendering.BibleTextBlock
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlin.math.ceil

/**
 * Where the verses of the blocks [bibleTextBlocks] has on screen were laid out, so a host can find a verse within
 * the chapter rather than only the block holding it.
 *
 * Only blocks the list has composed are known, and table blocks never report, so a verse in either is not found.
 * Placed blocks are snapshot state, so reading [verseBounds] in composition or a snapshotFlow updates as blocks are
 * laid out.
 */
@PlatformInternalApi
class BibleTextLayoutState internal constructor(
    private val blocksState: State<BibleTextBlocksState>,
) {
    private val placedBlocks = mutableStateMapOf<Int, PlacedBlock>()

    /**
     * The area [reference] takes up, relative to the top-left of the list item holding it, or null when the
     * block it starts in is not composed or not yet laid out with its current text. A verse wrapping onto another
     * line spans the block's full width.
     */
    fun verseBounds(reference: BibleReference): Rect? {
        val blocks = blocksState.value
        val index = blocks.indexOfBlockContaining(reference) ?: return null
        val placed = placedBlocks[index]?.takeIf { it.text === blocks.visibleBlocks[index].text } ?: return null
        return placed.boundsOf(reference)
    }

    /**
     * Scrolls [listState] so the line [reference] starts on sits at the top, with [leadingItemCount] being how many
     * of the host's own items come before the chapter's. When the verse's line is unknown, such as in a table
     * block, the top of its block is brought to the top instead, which still counts as found. Returns false only
     * when the chapter renders no block for it.
     */
    suspend fun scrollToVerse(
        listState: LazyListState,
        reference: BibleReference,
        leadingItemCount: Int,
    ): Boolean {
        val index = blocksState.value.indexOfBlockContaining(reference) ?: return false
        listState.scrollToItem(leadingItemCount + index)
        verseBounds(reference)?.let { listState.scrollBy(it.top) }
        return true
    }

    /**
     * Keeps the line at the top of [listState] in place when the chapter is restyled, which would otherwise reflow the
     * block holding it and leave the reader on another line. Runs until cancelled.
     */
    suspend fun keepReadingPosition(
        listState: LazyListState,
        leadingItemCount: Int,
    ) {
        var anchor: ReadingAnchor? = null
        snapshotFlow { blocksState.value.visibleBlocks }.collectLatest { blocks ->
            anchor?.let { restore(it, blocks, listState, leadingItemCount) }
            snapshotFlow { readingAnchor(blocks, listState, leadingItemCount, current = anchor) }
                .collect { anchor = it }
        }
    }

    /**
     * Where the reader is in [blocks], or [current] while the block at the top is still laid out with text a restyle
     * has since replaced, since that layout cannot tell where the reader is yet.
     */
    private fun readingAnchor(
        blocks: List<BibleTextBlock>,
        listState: LazyListState,
        leadingItemCount: Int,
        current: ReadingAnchor?,
    ): ReadingAnchor? {
        val index = listState.firstVisibleItemIndex - leadingItemCount
        val block = blocks.getOrNull(index) ?: return null
        val placed = placedBlocks[index] ?: return null
        if (placed.text !== block.text) return current
        val layout = placed.layout
        val y = listState.firstVisibleItemScrollOffset - placed.origin.y
        val line = layout.getLineForVerticalPosition(y)
        val lineHeight = layout.getLineBottom(line) - layout.getLineTop(line)
        val fractionIntoLine = if (lineHeight > 0f) (y - layout.getLineTop(line)) / lineHeight else 0f
        return ReadingAnchor(
            index,
            block.text.text,
            layout.getLineStart(line),
            fractionIntoLine.coerceIn(0f, 1f),
        )
    }

    /**
     * Scrolls back to [anchor] once its block is laid out with the restyled text, unless the list has been laid out
     * without that block on screen, since the reader has then scrolled away from it.
     */
    private suspend fun restore(
        anchor: ReadingAnchor,
        blocks: List<BibleTextBlock>,
        listState: LazyListState,
        leadingItemCount: Int,
    ) {
        val block = blocks.getOrNull(anchor.blockIndex)?.takeIf { it.text.text == anchor.text } ?: return
        val itemIndex = leadingItemCount + anchor.blockIndex
        val (placed, _) =
            snapshotFlow {
                val placed = placedBlocks[anchor.blockIndex]?.takeIf { it.text === block.text }
                placed to listState.layoutInfo.visibleItemsInfo.any { it.index == itemIndex }
            }.first { (placed, isVisible) -> placed != null || !isVisible }
        placed ?: return
        val layout = placed.layout
        val line = layout.getLineForOffset(anchor.offset)
        val top = placed.origin.y + layout.getLineTop(line)
        val bottom = placed.origin.y + layout.getLineBottom(line)
        val target = top + anchor.fractionIntoLine * (bottom - top)
        val firstPixel = ceil(top).toInt()
        val lastPixel = maxOf(firstPixel, ceil(bottom).toInt() - 1)
        listState.scrollToItem(itemIndex, target.toInt().coerceIn(firstPixel, lastPixel))
    }

    /** Records where block [blockIndex] was last laid out, replacing what was known of it. */
    internal fun place(
        blockIndex: Int,
        block: PlacedBlock,
    ) {
        placedBlocks[blockIndex] = block
    }

    /** Forgets block [blockIndex], unless it has since been placed with a layout other than [layout]. */
    internal fun remove(
        blockIndex: Int,
        layout: TextLayoutResult?,
    ) {
        if (placedBlocks[blockIndex]?.layout === layout) placedBlocks.remove(blockIndex)
    }
}

/** Keeps a [BibleTextLayoutState] measuring the chapter held by [blocksState] for as long as it is composed. */
@PlatformInternalApi
@Composable
fun rememberBibleTextLayoutState(blocksState: BibleTextBlocksState): BibleTextLayoutState {
    val currentBlocksState = rememberUpdatedState(blocksState)
    return remember { BibleTextLayoutState(currentBlocksState) }
}

/**
 * The character starting the line at the top of the list, and how far into that line the list was scrolled, as a
 * fraction of the line's height so it carries over to a line of another height.
 */
private class ReadingAnchor(
    val blockIndex: Int,
    val text: String,
    val offset: Int,
    val fractionIntoLine: Float,
)

/** A block's text as last laid out, with [origin] being where the text starts within its list item. */
internal class PlacedBlock(
    val text: AnnotatedString,
    val layout: TextLayoutResult,
    val origin: Offset,
)

private fun PlacedBlock.boundsOf(reference: BibleReference): Rect? {
    val ranges = text.selectedCharacterRanges(setOf(reference))
    if (ranges.isEmpty()) return null
    val first = ranges.minOf { it.first }
    val last = ranges.maxOf { it.last }
    val firstLine = layout.getLineForOffset(first)
    val lastLine = layout.getLineForOffset(last)
    val (left, right) =
        if (firstLine == lastLine) {
            val box = layout.getPathForRange(first, last + 1).getBounds()
            box.left to box.right
        } else {
            0f to layout.size.width.toFloat()
        }
    return Rect(left, layout.getLineTop(firstLine), right, layout.getLineBottom(lastLine)).translate(origin)
}
