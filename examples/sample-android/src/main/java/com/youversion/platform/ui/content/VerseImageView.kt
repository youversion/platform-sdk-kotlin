package com.youversion.platform.ui.content

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.youversion.platform.core.verseimages.models.VerseImage

/** The rendition of a [VerseImage] to download. */
internal enum class VerseImageSize(
    val queryValue: String?,
) {
    Card("card"),
    Full(null),
}

/**
 * Shows [image] at its own aspect ratio, or square when the ratio is unusable, with a spinner while it downloads and
 * a retry button if the download fails. A decorative image shows neither, takes no taps, and is hidden from
 * accessibility services. Retrying skips the disk cache, which keeps a failed response.
 *
 * @param image The image to show.
 * @param contentDescription What the image shows, read by accessibility services.
 * @param modifier The modifier for the image's frame.
 * @param size The rendition to download.
 * @param isDecorative Whether the image is a passive preview.
 * @param onClick Called when the user taps the downloaded image, or `null` when it is not tappable.
 * @param onClickLabel What a tap does, read by accessibility services.
 * @param accessibilityActions Extra actions that accessibility services offer on the image, even while it loads.
 */
@Composable
internal fun VerseImageView(
    image: VerseImage,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: VerseImageSize = VerseImageSize.Card,
    isDecorative: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String = "Open image",
    accessibilityActions: List<CustomAccessibilityAction> = emptyList(),
) {
    var retryCount by remember(image.id) { mutableIntStateOf(0) }
    val imageUrl =
        remember(image.imageUrl, size) {
            size.queryValue?.let {
                Uri
                    .parse(image.imageUrl)
                    .buildUpon()
                    .appendQueryParameter("size", it)
                    .build()
                    .toString()
            } ?: image.imageUrl
        }
    var imageState by remember(imageUrl, retryCount) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val aspectRatio = image.aspectRatio.toFloat().takeIf { it > 0f && it.isFinite() } ?: 1f
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    when {
                        isDecorative -> {
                            Modifier.clearAndSetSemantics {}
                        }

                        onClick != null -> {
                            Modifier
                                .clickable(
                                    enabled = imageState is AsyncImagePainter.State.Success,
                                    onClickLabel = onClickLabel,
                                    role = Role.Button,
                                    onClick = onClick,
                                ).semantics { customActions = accessibilityActions }
                        }

                        else -> {
                            Modifier.semantics(mergeDescendants = true) { customActions = accessibilityActions }
                        }
                    },
                ),
    ) {
        key(retryCount) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(imageUrl)
                        .diskCachePolicy(if (retryCount == 0) CachePolicy.ENABLED else CachePolicy.WRITE_ONLY)
                        .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                onState = { imageState = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!isDecorative) {
            when (imageState) {
                is AsyncImagePainter.State.Error -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.BrokenImage, contentDescription = null, modifier = Modifier.size(40.dp))
                        Text(
                            text = "The image didn’t load.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        OutlinedButton(onClick = { retryCount++ }) {
                            Text("Retry image")
                        }
                    }
                }

                is AsyncImagePainter.State.Success -> {}

                else -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
