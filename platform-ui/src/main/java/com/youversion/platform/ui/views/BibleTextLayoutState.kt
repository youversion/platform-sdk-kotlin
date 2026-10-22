package com.youversion.platform.ui.views

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.di.PlatformInternalApi

/**
 * Where the verses of the blocks [bibleTextBlocks] has on screen were laid out, so a host can find a verse within
 * the chapter rather than only the paragraph holding it.
 *
 * Only blocks the list has composed are known, and table blocks never report, so a verse in either is not found.
 */
@PlatformInternalApi
class BibleTextLayoutState internal constructor() {
    internal var blocksState: BibleTextBlocksState? = null
    private val placedBlocks = HashMap<Int, PlacedBlock>()

    /**
     * The area [reference] takes up, relative to the top-left of the list item holding it, or null when the
     * block it starts in is not composed. A verse wrapping onto another line spans the block's full width.
     */
    fun verseBounds(reference: BibleReference): Rect? {
        val index = blocksState?.indexOfBlockContaining(reference) ?: return null
        return placedBlocks[index]?.boundsOf(reference)
    }

    /**
     * Scrolls [listState] so the line [reference] starts on sits at the top, with [leadingItemCount] being how many
     * of the host's own items come before the chapter's. Returns false when the chapter renders no block for it.
     */
    suspend fun scrollToVerse(
        listState: LazyListState,
        reference: BibleReference,
        leadingItemCount: Int,
    ): Boolean {
        val index = blocksState?.indexOfBlockContaining(reference) ?: return false
        listState.scrollToItem(leadingItemCount + index)
        verseBounds(reference)?.let { listState.scrollBy(it.top) }
        return true
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
    val layoutState = remember { BibleTextLayoutState() }
    SideEffect { layoutState.blocksState = blocksState }
    return layoutState
}

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
