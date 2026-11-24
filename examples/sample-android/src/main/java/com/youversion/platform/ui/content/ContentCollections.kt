package com.youversion.platform.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.videos.models.VideoPlaylist

/** The display title of a collection, which may be missing from the response. */
internal val VideoPlaylist.displayTitle: String
    get() = title ?: "Collection"

/**
 * The video library: a featured collection followed by every collection, each shown by its first video's thumbnail.
 *
 * @param state The collection list.
 * @param videosState The videos loaded so far, for the thumbnails.
 * @param listState The list's scroll position, kept while the Images library shows.
 * @param onRetry Called when the user retries loading the collections.
 * @param onLoadVideos Called with video ids whose details should load.
 * @param onCollectionClick Called with the collection the user taps.
 */
@Composable
internal fun ContentCollections(
    state: ContentCollectionsState,
    videosState: ContentVideosState,
    listState: LazyListState,
    onRetry: () -> Unit,
    onLoadVideos: (List<String>) -> Unit,
    onCollectionClick: (VideoPlaylist) -> Unit,
) {
    val featured = state.collections.firstOrNull()

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        when {
            state.hasError -> {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "The collections didn’t load.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = onRetry) { Text("Retry") }
                    }
                }
            }

            state.isLoading -> {
                item {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator()
                    }
                }
            }

            featured == null -> {
                item {
                    Text(
                        text = "No collections available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            else -> {
                item {
                    SectionTitle("Featured")
                }
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable(role = Role.Button) { onCollectionClick(featured) },
                    ) {
                        CollectionThumbnail(featured, videosState, onLoadVideos, showsPlayIndicator = true)
                        CollectionLabel(featured)
                    }
                }
                item {
                    SectionTitle("All collections")
                }
                itemsIndexed(state.collections, key = {
                    index,
                    collection,
                    ->
                    collection.id ?: -index
                }) { _, collection ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onCollectionClick(collection) },
                    ) {
                        CollectionThumbnail(collection, videosState, onLoadVideos, modifier = Modifier.width(112.dp))
                        CollectionLabel(collection, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun CollectionLabel(
    collection: VideoPlaylist,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        Text(
            text = collection.displayTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        collection.total?.let {
            Text(
                text = if (it == 1) "1 video" else "$it videos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The collection's first video's thumbnail, with two cards peeking out behind it like a stack. */
@Composable
private fun CollectionThumbnail(
    collection: VideoPlaylist,
    videosState: ContentVideosState,
    onLoadVideos: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    showsPlayIndicator: Boolean = false,
) {
    val firstId = collection.videoIds?.firstOrNull()
    LaunchedEffect(firstId) {
        firstId?.let { onLoadVideos(listOf(it)) }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        listOf(18.dp to 0.35f, 10.dp to 0.6f).forEach { (inset, alpha) ->
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = inset)
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
                            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        ),
            )
        }
        VideoThumbnail(
            video = firstId?.let { videosState.videosByRequestedId[it] },
            isLoading =
                firstId != null && firstId !in videosState.videosByRequestedId && firstId !in videosState.failedIds,
            hasError = firstId in videosState.failedIds,
            onRetry = { firstId?.let { onLoadVideos(listOf(it)) } },
            showsPlayIndicator = showsPlayIndicator,
        )
    }
}
