package com.youversion.platform.reader.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.youversion.platform.reader.components.BibleReaderPassageSelectionState.Companion.Saver
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import kotlin.math.abs

@Composable
fun BibleReaderPassageSelection(
    bookAndChapter: String,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    scrollBehavior: BibleReaderPassageSelectionScrollBehavior? = null,
) {
    val passageSelectionDragModifier =
        if (scrollBehavior != null && !scrollBehavior.isPinned) {
            Modifier.draggable(
                orientation = Orientation.Vertical,
                state =
                    rememberDraggableState { delta ->
                        scrollBehavior.state.heightOffset += delta
                    },
                onDragStopped = { velocity ->
                    settlePassageSelection(
                        scrollBehavior.state,
                        velocity,
                        scrollBehavior.flingAnimationSpec,
                        scrollBehavior.snapAnimationSpec,
                    )
                },
            )
        } else {
            Modifier
        }

    Box(
        modifier =
            Modifier
                .then(passageSelectionDragModifier)
                .padding(vertical = 12.dp, horizontal = 24.dp)
                .background(
                    color = BibleReaderTheme.colorScheme.buttonPrimary,
                    shape = CircleShape,
                ).fillMaxWidth(),
    ) {
        IconButton(
            onClick = onPreviousChapter,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "Previous Chapter",
                tint = BibleReaderTheme.colorScheme.textPrimary,
            )
        }

        Text(
            text = bookAndChapter,
            style = BibleReaderTheme.typography.buttonLabelL,
            color = BibleReaderTheme.colorScheme.textPrimary,
            modifier = Modifier.align(Alignment.Center),
        )
        IconButton(
            onClick = onNextChapter,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Next Chapter",
                tint = BibleReaderTheme.colorScheme.textPrimary,
            )
        }
    }
}

private fun Modifier.adjustHeightOffsetLimit(scrollBehavior: BibleReaderPassageSelectionScrollBehavior?) =
    scrollBehavior?.state?.let {
        onSizeChanged { size ->
            val offset = size.height.toFloat() - it.heightOffset
            it.heightOffsetLimit = -offset
        }
    } ?: this

@Preview
@Composable
private fun Preview_BibleReader_PassageSelection() {
    BibleReaderMaterialTheme {
        BibleReaderPassageSelection(
            bookAndChapter = "Genesis 1",
            onPreviousChapter = {},
            onNextChapter = {},
        )
    }
}

class BibleReaderPassageSelectionState(
    initialHeightOffsetLimit: Float,
    initialHeightOffset: Float,
    initialContentOffset: Float,
) {
    var heightOffsetLimit = initialHeightOffsetLimit

    private var _heightOffset = mutableFloatStateOf(initialHeightOffset)
    var heightOffset: Float
        get() = _heightOffset.floatValue
        set(newOffset) {
            _heightOffset.floatValue =
                newOffset.coerceIn(minimumValue = heightOffsetLimit, maximumValue = 0f)
        }

    var contentOffset by mutableFloatStateOf(initialContentOffset)

    val collapsedFraction: Float
        get() =
            if (heightOffsetLimit != 0f) {
                heightOffset / heightOffsetLimit
            } else {
                0f
            }

    val overlappedFraction: Float
        get() =
            if (heightOffsetLimit != 0f) {
                1 -
                    (
                        (heightOffsetLimit - contentOffset).coerceIn(
                            minimumValue = heightOffsetLimit,
                            maximumValue = 0f,
                        ) / heightOffsetLimit
                    )
            } else {
                0f
            }

    companion object {
        /** The default [Saver] implementation for [BibleReaderPassageSelectionState]. */
        val Saver: Saver<BibleReaderPassageSelectionState, *> =
            listSaver(
                save = { listOf(it.heightOffsetLimit, it.heightOffset, it.contentOffset) },
                restore = {
                    BibleReaderPassageSelectionState(
                        initialHeightOffsetLimit = it[0],
                        initialHeightOffset = it[1],
                        initialContentOffset = it[2],
                    )
                },
            )
    }
}

@Composable
fun rememberBibleReaderPassageSelectionState(
    initialHeightOffsetLimit: Float = -Float.MAX_VALUE,
    initialHeightOffset: Float = 0f,
    initialContentOffset: Float = 0f,
): BibleReaderPassageSelectionState =
    rememberSaveable(saver = BibleReaderPassageSelectionState.Saver) {
        BibleReaderPassageSelectionState(initialHeightOffsetLimit, initialHeightOffset, initialContentOffset)
    }

object BibleReaderPassageSelectionDefaults {
    @Composable
    fun enterAlwaysScrollBehavior(
        state: BibleReaderPassageSelectionState = rememberBibleReaderPassageSelectionState(),
        canScroll: () -> Boolean = { true },
        snapAnimationSpec: AnimationSpec<Float>? =
            spring(
                dampingRatio = 1.7f,
                stiffness = 1600.0f,
            ),
        flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
        reverseLayout: Boolean = false,
    ): BibleReaderPassageSelectionScrollBehavior =
        remember(state, canScroll, snapAnimationSpec, flingAnimationSpec, reverseLayout) {
            EnterAlwaysScrollBehavior(
                state = state,
                snapAnimationSpec = snapAnimationSpec,
                flingAnimationSpec = flingAnimationSpec,
                canScroll = canScroll,
                reverseLayout = reverseLayout,
            )
        }
}

interface BibleReaderPassageSelectionScrollBehavior {
    /**
     * A [BibleReaderPassageSelectionState] that is attached to this behavior and is read and updated
     * when scrolling happens.
     */
    val state: BibleReaderPassageSelectionState

    /**
     * Indicates whether the passage selection is pinned.
     *
     * A pinned passage selection will stay fixed in place when content is scrolled and will not react
     * to any drag gestures.
     */
    val isPinned: Boolean

    /**
     * An optional [AnimationSpec] that defines how the passage selection snaps to either fully
     * collapsed or fully extended state when a fling or a drag scrolled it into an intermediate
     * position.
     */
    val snapAnimationSpec: AnimationSpec<Float>?

    /**
     * An optional [DecayAnimationSpec] that defined how to fling the passage selection when the user
     * flings the passage selection itself, or the content below it.
     */
    val flingAnimationSpec: DecayAnimationSpec<Float>?

    /**
     * A [NestedScrollConnection] that should be attached to a [Modifier.nestedScroll] in order to
     * keep track of the scroll events.
     */
    val nestedScrollConnection: NestedScrollConnection
}

private class EnterAlwaysScrollBehavior(
    override val state: BibleReaderPassageSelectionState,
    override val snapAnimationSpec: AnimationSpec<Float>? = null,
    override val flingAnimationSpec: DecayAnimationSpec<Float>? = null,
    val canScroll: () -> Boolean = { true },
    val reverseLayout: Boolean = false,
) : BibleReaderPassageSelectionScrollBehavior {
    override val isPinned: Boolean = false
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!canScroll()) return Offset.Zero
                val prevHeightOffset = state.heightOffset
                state.heightOffset += available.y
                Logger.d("State Height Offset: ${state.heightOffset}")
                // The state's heightOffset is coerce in a minimum value of heightOffsetLimit and a
                // maximum value 0f, so we check if its value was actually changed after the
                // available.y was added to it in order to tell if the top app bar is currently
                // collapsing or expanding.
                // Note that when the content was set with a revered layout, we always return a
                // zero offset.
                return if (!reverseLayout && prevHeightOffset != state.heightOffset) {
                    // We're in the middle of top app bar collapse or expand.
                    // Consume only the scroll on the Y axis.
                    available.copy(x = 0f)
                } else {
                    Offset.Zero
                }
            }

//            override fun onPostScroll(
//                consumed: Offset,
//                available: Offset,
//                source: NestedScrollSource,
//            ): Offset {
//                if (!canScroll()) return Offset.Zero
//                state.contentOffset += consumed.y
//                if (!reverseLayout) state.heightOffset += consumed.y
//                return Offset.Zero
//            }

//            override suspea
            //            nd fun onPostFling(
//                consumed: Velocity,
//                available: Velocity,
//            ): Velocity {
//                if (
//                    available.y > 0f &&
//                    (state.heightOffset == 0f || state.heightOffset == state.heightOffsetLimit)
//                ) {
//                    // Reset the total content offset to zero when scrolling all the way down.
//                    // This will eliminate some float precision inaccuracies.
//                    state.contentOffset = 0f
//                }
//                val superConsumed = super.onPostFling(consumed, available)
//                return superConsumed +
//                    settlePassageSelection(state, available.y, flingAnimationSpec, snapAnimationSpec)
//            }
        }
}

private suspend fun settlePassageSelection(
    state: BibleReaderPassageSelectionState,
    velocity: Float,
    flingAnimationSpec: DecayAnimationSpec<Float>?,
    snapAnimationSpec: AnimationSpec<Float>?,
): Velocity {
    // Check if the app bar is completely collapsed/expanded. If so, no need to settle the app bar,
    // and just return Zero Velocity.
    // Note that we don't check for 0f due to float precision with the collapsedFraction
    // calculation.
    if (state.collapsedFraction < 0.01f || state.collapsedFraction == 1f) {
        return Velocity.Zero
    }

    var remainingVelocity = velocity
    // In case there is an initial velocity that was left after a previous user fling, animate to
    // continue the motion to expand or collapse the app bar.
    if (flingAnimationSpec != null && abs(velocity) > 1f) {
        var lastValue = 0f
        AnimationState(initialValue = 0f, initialVelocity = velocity)
            .animateDecay(flingAnimationSpec) {
                val delta = value - lastValue
                val initialHeightOffset = state.heightOffset
                state.heightOffset = initialHeightOffset + delta
                val consumed = abs(initialHeightOffset - state.heightOffset)
                lastValue = value
                remainingVelocity = this.velocity

                // avoid rounding errors and stop if anything is unconsumed
                if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
            }
    }

    // Snap if animation specs were provided.
    if (snapAnimationSpec != null) {
        if (state.heightOffset < 0 && state.heightOffset > state.heightOffsetLimit) {
            AnimationState(initialValue = state.heightOffset).animateTo(
                if (state.collapsedFraction < 0.5f) {
                    0f
                } else {
                    state.heightOffsetLimit
                },
                animationSpec = snapAnimationSpec,
            ) {
                state.heightOffset = value
            }
        }
    }

    return Velocity(0f, remainingVelocity)
}
