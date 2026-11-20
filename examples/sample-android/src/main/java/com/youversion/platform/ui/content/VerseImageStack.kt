package com.youversion.platform.ui.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.youversion.platform.core.verseimages.models.VerseImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

private const val VISIBLE_CARD_COUNT = 3
private const val INDICATOR_DOT_COUNT = 7

/**
 * A card that has been swiped off the top of the stack: it flies out over the stack, then slides back in behind it.
 */
private class CardFlight(
    val order: Int,
    startOffset: Offset,
) {
    val offset = Animatable(startOffset, Offset.VectorConverter)
    var isReturning by mutableStateOf(false)

    val zIndex: Float
        get() = if (isReturning) -(order + VISIBLE_CARD_COUNT).toFloat() else order.toFloat()
}

/**
 * Shows [images] as a stack of cards. Swiping the top card sideways sends it to the back of the stack. Tapping it
 * calls [onImageClick], or also sends it to the back when there is none. Accessibility services get "Next image" and
 * "Previous image" actions on the top card instead of the swipe. Cards behind the top one are decorative previews.
 *
 * @param images The images to show. It must not be empty.
 * @param titleOf The passage title read out for an image.
 * @param modifier The modifier for the stack.
 * @param onSelectionChange Called with the image on top whenever it changes, including the first one.
 * @param onImageClick Called when the user taps the image on top, or `null` to shuffle on a tap.
 */
@Composable
fun VerseImageStack(
    images: List<VerseImage>,
    titleOf: (VerseImage) -> String,
    modifier: Modifier = Modifier,
    onSelectionChange: (VerseImage) -> Unit = {},
    onImageClick: ((VerseImage) -> Unit)? = null,
) {
    var selectedImageId by rememberSaveable { mutableStateOf(images.first().id) }
    val selectedIndex = images.indexOfFirst { it.id == selectedImageId }.coerceAtLeast(0)
    val selectedImage = images[selectedIndex]
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)
    LaunchedEffect(selectedImage) { currentOnSelectionChange(selectedImage) }

    val scope = rememberCoroutineScope()
    val flights = remember { mutableStateMapOf<String, CardFlight>() }
    var shuffleCount by remember { mutableIntStateOf(0) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val localDensity = LocalDensity.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            val stackWidth = constraints.maxWidth.toFloat()

            fun settle() {
                settleJob =
                    scope.launch {
                        animate(dragOffsetX, 0f, animationSpec = spring()) { value, _ -> dragOffsetX = value }
                    }
            }

            fun shuffle(direction: Float) {
                val currentId = images[selectedIndex].id
                val nextId = images[(selectedIndex + 1) % images.size].id
                val flight = CardFlight(order = ++shuffleCount, startOffset = Offset(dragOffsetX, 0f))
                flights.remove(nextId)
                flights[currentId] = flight
                dragOffsetX = 0f
                selectedImageId = nextId
                scope.launch {
                    with(localDensity) {
                        flight.offset.animateTo(
                            Offset(direction * (stackWidth + 24.dp.toPx()), 12.dp.toPx()),
                            tween(durationMillis = 200, easing = EaseOut),
                        )
                    }
                    flight.isReturning = true
                    flight.offset.animateTo(Offset.Zero, tween(durationMillis = 300, easing = EaseOut))
                    if (flights[currentId] === flight) flights.remove(currentId)
                }
            }

            fun showPrevious() {
                val previousId = images[(selectedIndex - 1 + images.size) % images.size].id
                flights.remove(previousId)
                dragOffsetX = 0f
                selectedImageId = previousId
            }

            images.forEachIndexed { index, image ->
                val depth = (index - selectedIndex + images.size) % images.size
                val flight = flights[image.id]
                if (depth >= VISIBLE_CARD_COUNT && flight == null) return@forEachIndexed
                key(image.id) {
                    val appearance = remember { Animatable(if (depth == 0) 1f else 0f) }
                    LaunchedEffect(Unit) { appearance.animateTo(1f, tween(durationMillis = 300)) }
                    val isFlyingOut = flight != null && !flight.isReturning
                    val stackDepth = if (isFlyingOut) 0 else min(depth, VISIBLE_CARD_COUNT - 1)
                    val restingAngle = if (index % 2 == 0) 4f else -5f
                    VerseImageView(
                        image = image,
                        contentDescription = titleOf(image),
                        isDecorative = depth != 0,
                        accessibilityActions =
                            if (images.size > 1) {
                                listOf(
                                    CustomAccessibilityAction("Next image") {
                                        shuffle(-1f)
                                        true
                                    },
                                    CustomAccessibilityAction("Previous image") {
                                        showPrevious()
                                        true
                                    },
                                )
                            } else {
                                emptyList()
                            },
                        onClick = {
                            when {
                                dragOffsetX != 0f -> Unit
                                onImageClick != null -> onImageClick(image)
                                images.size > 1 -> shuffle(-1f)
                            }
                        },
                        modifier =
                            Modifier
                                .zIndex(flight?.zIndex ?: -depth.toFloat())
                                .then(
                                    if (depth == 0 && images.size > 1) {
                                        Modifier.pointerInput(image.id, images, stackWidth) {
                                            val velocityTracker = VelocityTracker()
                                            detectHorizontalDragGestures(
                                                onDragStart = {
                                                    settleJob?.cancel()
                                                    velocityTracker.resetTracking()
                                                },
                                                onDragEnd = {
                                                    val predictedX =
                                                        dragOffsetX + velocityTracker.calculateVelocity().x * 0.2f
                                                    if (abs(predictedX) > stackWidth * 0.25f) {
                                                        shuffle(sign(predictedX))
                                                    } else {
                                                        settle()
                                                    }
                                                },
                                                onDragCancel = { settle() },
                                            ) { change, dragAmount ->
                                                change.consume()
                                                dragOffsetX += dragAmount
                                                velocityTracker.addPosition(
                                                    change.uptimeMillis,
                                                    Offset(dragOffsetX, 0f),
                                                )
                                            }
                                        }
                                    } else {
                                        Modifier
                                    },
                                ).graphicsLayer {
                                    val offset =
                                        flight?.offset?.value ?: Offset(if (depth == 0) dragOffsetX else 0f, 0f)
                                    val isOnTop = depth == 0 || isFlyingOut
                                    val stackOffsetX = if (stackDepth == 1) 7.dp.toPx() else -stackDepth * 3.dp.toPx()
                                    translationX = offset.x + stackOffsetX
                                    translationY = offset.y + stackDepth * 6.dp.toPx()
                                    rotationZ = if (isOnTop) offset.x / density / 24f else restingAngle
                                    scaleX = 1f - stackDepth * 0.035f
                                    scaleY = scaleX
                                    alpha =
                                        if (flight?.isReturning == true && depth >= VISIBLE_CARD_COUNT) {
                                            (abs(offset.x) / stackWidth).coerceIn(0f, 1f)
                                        } else {
                                            appearance.value
                                        }
                                }.shadow(12.dp, RoundedCornerShape(18.dp), clip = false),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val start = (selectedIndex - INDICATOR_DOT_COUNT / 2).coerceAtMost(images.size - INDICATOR_DOT_COUNT)
            images.drop(start.coerceAtLeast(0)).take(INDICATOR_DOT_COUNT).forEach { image ->
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .background(
                                color =
                                    if (image.id == selectedImage.id) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    },
                                shape = CircleShape,
                            ),
                )
            }
        }

        Text(
            text =
                when {
                    images.size == 1 -> "1 of 1"
                    onImageClick == null -> "${selectedIndex + 1} of ${images.size} · Tap or swipe for more"
                    else -> "${selectedIndex + 1} of ${images.size} · Swipe for more"
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
