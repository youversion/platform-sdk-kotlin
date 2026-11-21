package com.youversion.platform.ui.content

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.youversion.platform.core.videos.models.Video

/** The thumbnail's aspect ratio, or the usual ratio for the video's orientation when the thumbnail has no valid size. */
internal val Video.displayAspectRatio: Float
    get() = thumbnail?.aspectRatio?.toFloat() ?: if (orientation == "portrait") 9f / 16 else 16f / 9

/** [seconds] as a clock duration, such as "5:15" or "1:02:03". */
internal fun formatRuntime(seconds: Int): String = DateUtils.formatElapsedTime(seconds.toLong())

/**
 * Shows [video]'s thumbnail. While the video's details load it shows a spinner, and when they or the image fail it
 * shows a retry button.
 *
 * @param video The video, or `null` until its details load.
 * @param isLoading Whether the video's details are loading.
 * @param hasError Whether the video's details failed to load.
 * @param onRetry Called when the user retries loading the video's details.
 * @param modifier The modifier for the thumbnail's frame.
 * @param showsPlayIndicator Whether to draw a play button over the image.
 * @param runtimeSeconds The running time to show in a badge, or `null` for none.
 */
@Composable
internal fun VideoThumbnail(
    video: Video?,
    isLoading: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    showsPlayIndicator: Boolean = false,
    runtimeSeconds: Int? = null,
) {
    var retryCount by remember(video?.id) { mutableIntStateOf(0) }
    val imageUrl = video?.thumbnail?.url
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(video?.displayAspectRatio ?: (16f / 9))
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        when {
            hasError -> {
                RetryButton(onClick = onRetry)
            }

            isLoading -> {
                CircularProgressIndicator()
            }

            imageUrl == null -> {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                var imageState by remember(imageUrl, retryCount) {
                    mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
                }
                key(retryCount) {
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
                                .data(imageUrl)
                                .diskCachePolicy(if (retryCount == 0) CachePolicy.ENABLED else CachePolicy.WRITE_ONLY)
                                .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onState = { imageState = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (imageState is AsyncImagePainter.State.Error) {
                    RetryButton(onClick = { retryCount++ })
                }
                if (showsPlayIndicator) {
                    Icon(
                        Icons.Filled.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .size(44.dp),
                    )
                }
            }
        }
        if (runtimeSeconds != null) {
            Text(
                text = formatRuntime(runtimeSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.8f), CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun RetryButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.Refresh, contentDescription = "Retry loading thumbnail")
    }
}
