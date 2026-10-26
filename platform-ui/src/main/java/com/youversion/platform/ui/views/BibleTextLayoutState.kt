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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

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

    private fun readingAnchor(
        blocks: List<BibleTextBlock>,
        listState: LazyListState,
        leadingItemCount: Int,
        current: ReadingAnchor?,
    ): ReadingAnchor? {
        val index = listState.firstVisibleItemIndex - leadingItemCount
        val block = blocks.getOrNull(index) ?: return null
        val placed = placedBlocks[index] ?: return null
        // Laid out with text a restyle has since replaced, the block cannot tell where the reader is yet.
        if (placed.text !== block.text) return current
        val y = listState.firstVisibleItemScrollOffset - placed.origin.y
        val line = placed.layout.getLineForVerticalPosition(y)
        return ReadingAnchor(
            index,
            block.text.text,
            placed.layout.getLineStart(line),
            y - placed.layout.getLineTop(line),
        )
    }

    private suspend fun restore(
        anchor: ReadingAnchor,
        blocks: List<BibleTextBlock>,
        listState: LazyListState,
        leadingItemCount: Int,
    ) {
        val block = blocks.getOrNull(anchor.blockIndex)?.takeIf { it.text.text == anchor.text } ?: return
        listState.scrollToItem(leadingItemCount + anchor.blockIndex)
        val placed =
            snapshotFlow { placedBlocks[anchor.blockIndex]?.takeIf { it.text === block.text } }.filterNotNull().first()
        val lineTop = placed.layout.getLineTop(placed.layout.getLineForOffset(anchor.offset))
        listState.scrollBy(placed.origin.y + lineTop + anchor.offsetIntoLine)
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

/** The character starting the line at the top of the list, and how far into that line the list was scrolled. */
private class ReadingAnchor(
    val blockIndex: Int,
    val text: String,
    val offset: Int,
    val offsetIntoLine: Float,
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
