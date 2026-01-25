package com.youversion.platform.reader.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.youversion.platform.reader.components.PassageSelectionState.Companion.Saver
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderPassageSelection(
    bookAndChapter: String,
    onReferenceClick: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    bottomBarScrollBehavior: BottomAppBarScrollBehavior? = null,
    scrollBehavior: PassageSelectionScrollBehavior? = null,
) {
    val alpha = 1f - (scrollBehavior?.state?.collapsedFraction ?: 0f)
    Box(
        modifier =
            Modifier
                .padding(vertical = 4.dp, horizontal = 24.dp)
                .dropShadow(RectangleShape) {
                    this.radius = 48f

                    this.offset = Offset(0f, 0f)
                    this.color = Color.Black.copy(alpha = alpha * 0.2f)
                }.clip(CircleShape)
                .background(
                    color = BibleReaderTheme.colorScheme.canvasPrimary,
                    shape = CircleShape,
                ).clickable(
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onReferenceClick,
                ).fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)

                    val heightOffsetLimit =
                        bottomBarScrollBehavior?.state?.heightOffsetLimit ?: -placeable.height.toFloat()
                    scrollBehavior?.state?.heightOffsetLimit = heightOffsetLimit

                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
    ) {
        IconButton(
            onClick = onPreviousChapter,
            enabled = alpha > 0.1f,
            modifier =
                Modifier
                    .alpha(alpha)
                    .align(Alignment.CenterStart),
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
            enabled = alpha > 0.1f,
            modifier =
                Modifier
                    .alpha(alpha)
                    .align(Alignment.CenterEnd),
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Next Chapter",
                tint = BibleReaderTheme.colorScheme.textPrimary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview_BibleReader_PassageSelection() {
    BibleReaderMaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BibleReaderTheme.colorScheme.canvasSecondary)
                    .padding(16.dp),
        ) {
            BibleReaderPassageSelection(
                bookAndChapter = "Genesis 1",
                onReferenceClick = {},
                onPreviousChapter = {},
                onNextChapter = {},
                bottomBarScrollBehavior = null,
                scrollBehavior = null,
            )
        }
    }
}

fun PassageSelectionState(
    initialHeightOffsetLimit: Float,
    initialHeightOffset: Float,
    initialContentOffset: Float,
): PassageSelectionState =
    PassageSelectionStateImpl(
        initialHeightOffsetLimit,
        initialHeightOffset,
        initialContentOffset,
    )

interface PassageSelectionState {
    var heightOffsetLimit: Float

    var heightOffset: Float

    var contentOffset: Float

    val collapsedFraction: Float

    companion object {
        /** The default [Saver] implementation for [PassageSelectionState]. */
        val Saver: Saver<PassageSelectionState, *> =
            listSaver(
                save = { listOf(it.heightOffsetLimit, it.heightOffset, it.contentOffset) },
                restore = {
                    PassageSelectionState(
                        initialHeightOffsetLimit = it[0],
                        initialHeightOffset = it[1],
                        initialContentOffset = it[2],
                    )
                },
            )
    }
}

private class PassageSelectionStateImpl(
    initialHeightOffsetLimit: Float,
    initialHeightOffset: Float,
    initialContentOffset: Float,
) : PassageSelectionState {
    override var heightOffsetLimit by mutableFloatStateOf(initialHeightOffsetLimit)

    private var _heightOffset = mutableFloatStateOf(initialHeightOffset)
    override var heightOffset: Float
        get() = _heightOffset.floatValue
        set(newOffset) {
            _heightOffset.floatValue =
                newOffset.coerceIn(minimumValue = heightOffsetLimit, maximumValue = 0f)
        }

    override var contentOffset by mutableFloatStateOf(initialContentOffset)

    override val collapsedFraction: Float
        get() =
            if (heightOffsetLimit != 0f) {
                heightOffset / heightOffsetLimit
            } else {
                0f
            }
}

@Composable
fun rememberPassageSelectionState(
    initialHeightOffsetLimit: Float = -Float.MAX_VALUE,
    initialHeightOffset: Float = 0f,
    initialContentOffset: Float = 0f,
): PassageSelectionState =
    rememberSaveable(saver = Saver) {
        PassageSelectionState(initialHeightOffsetLimit, initialHeightOffset, initialContentOffset)
    }

object PassageSelectionDefaults {
    @Composable
    fun fadeAlwaysScrollBehavior(
        state: PassageSelectionState = rememberPassageSelectionState(),
        canScroll: () -> Boolean = { true },
        snapAnimationSpec: AnimationSpec<Float>? =
            spring(
                dampingRatio = 1.7f,
                stiffness = 1600.0f,
            ),
        flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
    ): PassageSelectionScrollBehavior =
        remember(state, canScroll, snapAnimationSpec, flingAnimationSpec) {
            FadeAlwaysScrollBehavior(
                state = state,
                snapAnimationSpec = snapAnimationSpec,
                flingAnimationSpec = flingAnimationSpec,
                canScroll = canScroll,
            )
        }
}

interface PassageSelectionScrollBehavior {
    /**
     * A [PassageSelectionState] that is attached to this behavior and is read and updated
     * when scrolling happens.
     */
    val state: PassageSelectionState

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

private class FadeAlwaysScrollBehavior(
    override val state: PassageSelectionState,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true },
) : PassageSelectionScrollBehavior {
    override val isPinned: Boolean = false
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!canScroll()) return Offset.Zero
                state.contentOffset += consumed.y
                state.heightOffset += consumed.y
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                if (
                    available.y > 0f &&
                    (state.heightOffset == 0f || state.heightOffset == state.heightOffsetLimit)
                ) {
                    // Reset the total content offset to zero when scrolling all the way down.
                    // This will eliminate some float precision inaccuracies.
                    state.contentOffset = 0f
                }
                val superConsumed = super.onPostFling(consumed, available)
                return superConsumed +
                    settlePassageSelection(state, available.y, flingAnimationSpec, snapAnimationSpec)
            }
        }
}

private suspend fun settlePassageSelection(
    state: PassageSelectionState,
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
                targetValue =
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
