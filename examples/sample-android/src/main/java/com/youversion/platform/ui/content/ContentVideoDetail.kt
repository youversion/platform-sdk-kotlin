package com.youversion.platform.ui.content

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.youversion.platform.core.videos.models.Video
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination

/**
 * Plays [video] with its title, collection, running time and description. Playback stops when the screen stops,
 * resumes when it returns if it was playing, and keeps its position across configuration changes and process death.
 * Turning the device to landscape while the video plays shows it full screen until the user closes it or turns the
 * device back.
 *
 * @param video The video to play.
 * @param collectionTitle The title of the collection the video was opened from, or `null`.
 * @param onBack Called when the user taps the back arrow.
 * @param onDestinationClick Called when the user picks a tab in the bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContentVideoDetail(
    video: Video,
    collectionTitle: String?,
    onBack: () -> Unit,
    onDestinationClick: (SampleDestination) -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val playback =
        viewModel { ContentVideoPlayerViewModel(application, createSavedStateHandle(), video.playbackSources) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isFullScreen by rememberSaveable { mutableStateOf(false) }
    var hasDismissedFullScreen by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, playback) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        playback.start()
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        playback.save()
                    }

                    Lifecycle.Event.ON_STOP -> {
                        playback.stop()
                    }

                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isLandscape, playback.isPlaying, playback.hasError) {
        when {
            !isLandscape -> {
                isFullScreen = false
                hasDismissedFullScreen = false
            }

            playback.hasError -> {
                isFullScreen = false
            }

            playback.isPlaying && !hasDismissedFullScreen -> {
                isFullScreen = true
            }
        }
    }

    val playerSurface =
        remember(playback) {
            movableContentOf { modifier: Modifier ->
                AndroidView(
                    factory = { PlayerView(it) },
                    update = {
                        it.player = playback.player
                        it.keepScreenOn = playback.isPlaying
                    },
                    onRelease = { it.player = null },
                    modifier = modifier,
                )
            }
        }

    if (isFullScreen && playback.hasSource && !playback.hasError) {
        val closeFullScreen = {
            hasDismissedFullScreen = true
            isFullScreen = false
        }
        BackHandler(onBack = closeFullScreen)
        HiddenSystemBars()
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            playerSurface(Modifier.fillMaxSize())
            IconButton(
                onClick = closeFullScreen,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding(),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        video.shareUrl?.takeIf { it.isNotBlank() }?.let { shareUrl ->
                            IconButton(onClick = { shareText(context, shareUrl) }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share video")
                            }
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
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
            ) {
                when {
                    !playback.hasSource -> {
                        PlaybackMessage(video, "Video unavailable", onRetry = null)
                    }

                    playback.hasError -> {
                        PlaybackMessage(video, "This video couldn’t play.", onRetry = playback::retry)
                    }

                    else -> {
                        playerSurface(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(video.displayAspectRatio)
                                .background(Color.Black),
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    video.title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    listOfNotNull(collectionTitle, video.runtimeSeconds?.let(::formatRuntime))
                        .takeIf { it.isNotEmpty() }
                        ?.let {
                            Text(
                                text = it.joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    video.description?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

/** Opens the share sheet with [text]. */
internal fun shareText(
    context: Context,
    text: String,
) {
    val intent =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, null))
}

/** Hides the status and navigation bars while it is shown, like a full-screen player. */
@Composable
private fun HiddenSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

@Composable
private fun PlaybackMessage(
    video: Video,
    message: String,
    onRetry: (() -> Unit)?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(video.displayAspectRatio)
                .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            onRetry?.let { OutlinedButton(onClick = it) { Text("Retry") } }
        }
    }
}
