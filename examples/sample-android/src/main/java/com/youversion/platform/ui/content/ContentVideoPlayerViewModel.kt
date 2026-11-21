package com.youversion.platform.ui.content

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.youversion.platform.core.videos.models.VideoPlaybackSource

private const val POSITION_KEY = "positionMillis"
private const val PLAY_WHEN_READY_KEY = "playWhenReady"
private const val SOURCE_INDEX_KEY = "sourceIndex"

/** The streams to try in order: every HLS source, then every MP4 source, used exactly as returned. */
internal val List<VideoPlaybackSource>.playbackCandidates: List<VideoPlaybackSource>
    get() = filter { it.format == "hls" } + filter { it.format == "mp4" }

/**
 * Owns the video screen's player, so it survives configuration changes. The player exists only while the screen is
 * started: [start] builds it and [stop] releases its decoders. The position, whether to play, and the stream in use
 * are kept in [savedStateHandle] by [save], so playback resumes in the same place after process death. A stream that
 * fails falls back to the next of [sources]' [playbackCandidates] before an error shows.
 */
internal class ContentVideoPlayerViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    sources: List<VideoPlaybackSource>,
) : AndroidViewModel(application) {
    private val candidates = sources.playbackCandidates

    /** The player while the screen is started, or `null`. */
    var player by mutableStateOf<ExoPlayer?>(null)
        private set

    /** Whether the video is playing. */
    var isPlaying by mutableStateOf(false)
        private set

    /** Whether every stream failed to play. */
    var hasError by mutableStateOf(false)
        private set

    /** Whether the video has any stream the player supports. */
    val hasSource: Boolean = candidates.isNotEmpty()

    private val listener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@ContentVideoPlayerViewModel.isPlaying = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                val player = player ?: return
                val nextIndex = sourceIndex + 1
                if (nextIndex < candidates.size) {
                    savedStateHandle[SOURCE_INDEX_KEY] = nextIndex
                    player.setMediaItem(mediaItem(candidates[nextIndex]), player.currentPosition)
                    player.prepare()
                } else {
                    hasError = true
                }
            }
        }

    private val sourceIndex: Int
        get() = savedStateHandle[SOURCE_INDEX_KEY] ?: 0

    /** Builds the player and resumes playback where it was saved. */
    fun start() {
        if (player != null || !hasSource || hasError) return
        val audioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
        player =
            ExoPlayer
                .Builder(getApplication())
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    addListener(listener)
                    setMediaItem(mediaItem(candidates[sourceIndex]), savedStateHandle[POSITION_KEY] ?: 0L)
                    playWhenReady = savedStateHandle[PLAY_WHEN_READY_KEY] ?: true
                    prepare()
                }
    }

    /** Saves the position and whether to play. */
    fun save() {
        val player = player ?: return
        savedStateHandle[POSITION_KEY] = player.currentPosition
        savedStateHandle[PLAY_WHEN_READY_KEY] = player.playWhenReady
    }

    /** Saves the playback state and releases the player. */
    fun stop() {
        save()
        player?.run {
            removeListener(listener)
            release()
        }
        player = null
        isPlaying = false
    }

    /** Tries every stream again from the first. */
    fun retry() {
        stop()
        hasError = false
        savedStateHandle[SOURCE_INDEX_KEY] = 0
        savedStateHandle[PLAY_WHEN_READY_KEY] = true
        start()
    }

    override fun onCleared() {
        stop()
    }

    private fun mediaItem(source: VideoPlaybackSource): MediaItem =
        MediaItem
            .Builder()
            .setUri(source.url)
            .setMimeType(if (source.format == "hls") MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
            .build()
}
