package com.youversion.platform.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.videos.models.Video
import com.youversion.platform.core.videos.models.VideoPlaylist
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import java.util.Locale

/**
 * Shows [collection]'s videos in playlist order, with its total, and with its running time and languages once every
 * video has loaded. The play card always plays the playlist's first video, and Share is available once every video
 * has loaded with a share link.
 *
 * @param collection The collection the user opened.
 * @param videosState The videos loaded so far.
 * @param onLoadVideos Called with video ids whose details should load.
 * @param onBack Called when the user taps the back arrow.
 * @param onDestinationClick Called when the user picks a tab in the bottom bar.
 * @param onVideoClick Called with the playlist id of the video the user taps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContentCollectionDetail(
    collection: VideoPlaylist,
    videosState: ContentVideosState,
    onLoadVideos: (List<String>) -> Unit,
    onBack: () -> Unit,
    onDestinationClick: (SampleDestination) -> Unit,
    onVideoClick: (requestedId: String) -> Unit,
) {
    val context = LocalContext.current
    val state = videosState
    val videoIds = collection.videoIds.orEmpty()
    val indexedVideos =
        videoIds.withIndex().mapNotNull { (index, id) ->
            state.videosByRequestedId[id]?.let { index to it }
        }
    val videos = indexedVideos.map { it.second }
    val isLoading = videoIds.any { it in state.loadingIds }
    val hasError = videoIds.any { it in state.failedIds }
    val hasEveryVideo = videoIds.isNotEmpty() && videos.size == videoIds.size
    val totalRuntimeSeconds =
        videos
            .map { it.runtimeSeconds }
            .takeIf { hasEveryVideo && null !in it }
            ?.sumOf { it ?: 0 }
    val shareUrls = videos.mapNotNull { it.shareUrl?.takeIf(String::isNotBlank) }.distinct()
    val canShare = hasEveryVideo && videos.all { !it.shareUrl.isNullOrBlank() }
    val languages =
        videos
            .takeIf { hasEveryVideo }
            .orEmpty()
            .mapNotNull { it.languageTag }
            .distinct()
            .map { Locale.forLanguageTag(it).getDisplayName(Locale.getDefault()).ifEmpty { it } }
            .sorted()

    LaunchedEffect(collection) {
        onLoadVideos(videoIds)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        enabled = canShare,
                        onClick = {
                            val lines = listOfNotNull(collection.title) + shareUrls
                            shareText(context, lines.joinToString("\n"))
                        },
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share collection")
                    }
                },
            )
        },
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Content,
                onDestinationClick = onDestinationClick,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(20.dp),
            modifier = Modifier.padding(innerPadding),
        ) {
            item {
                val firstId = videoIds.firstOrNull()
                val first = firstId?.let { state.videosByRequestedId[it] }
                VideoThumbnail(
                    video = first,
                    isLoading = firstId != null && first == null && firstId !in state.failedIds,
                    hasError = firstId in state.failedIds,
                    onRetry = { onLoadVideos(videoIds) },
                    showsPlayIndicator = true,
                    runtimeSeconds = first?.runtimeSeconds,
                    modifier =
                        if (first == null || firstId == null) {
                            Modifier
                        } else {
                            Modifier
                                .clickable(role = Role.Button, onClickLabel = "Play") { onVideoClick(firstId) }
                                .semantics { contentDescription = "Play ${first.title ?: "video"}" }
                        },
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = collection.displayTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    collection.description?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    collection.total?.let { Metadata("Videos", it.toString()) }
                    totalRuntimeSeconds?.let { Metadata("Duration", formatRuntime(it)) }
                    if (languages.isNotEmpty()) Metadata("Language", languages.joinToString())
                }
            }
            item {
                HorizontalDivider()
            }
            item {
                Text(
                    text = "Videos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator()
                        }

                        hasError -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Some videos didn’t load.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedButton(onClick = { onLoadVideos(videoIds) }) { Text("Retry") }
                            }
                        }

                        videoIds.isEmpty() -> {
                            Text(
                                text = "This collection has no videos.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            items(indexedVideos, key = { it.first }) { (index, video) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onVideoClick(videoIds[index]) },
                ) {
                    VideoThumbnail(
                        video = video,
                        isLoading = false,
                        hasError = false,
                        onRetry = {},
                        runtimeSeconds = video.runtimeSeconds,
                        modifier = Modifier.width(120.dp),
                    )
                    Text(
                        text = video.title ?: "Video",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.Metadata(
    title: String,
    value: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .padding(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
