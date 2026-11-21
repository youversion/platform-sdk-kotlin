package com.youversion.platform.ui.content

import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.core.videos.models.Video
import com.youversion.platform.core.videos.models.VideoPlaylist
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, PlatformInternalApi::class)
class ContentVideosViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun video(responseId: String) =
        Video(id = responseId, playbackSources = emptyList(), playbackPreviewSources = emptyList())

    @Test
    fun retryAfterPartialFailureKeepsLoadedVideosAndFetchesOnlyFailedOnes() =
        runTest(dispatcher) {
            val requests = mutableListOf<String>()
            var failsB = true
            val viewModel =
                ContentVideosViewModel(
                    playlistsPage = { _, _ -> emptyList<VideoPlaylist>() to null },
                    videoDetail = { id ->
                        requests += id
                        if (id == "b" && failsB) error("offline")
                        video("response-$id")
                    },
                )

            viewModel.loadVideos(listOf("a", "b", "c"))
            advanceUntilIdle()

            assertEquals(setOf("b"), viewModel.videosState.value.failedIds)
            assertEquals(
                mapOf("a" to "response-a", "c" to "response-c"),
                viewModel.videosState.value.videosByRequestedId
                    .mapValues { it.value.id },
            )

            failsB = false
            viewModel.loadVideos(listOf("a", "b", "c"))
            advanceUntilIdle()

            assertEquals(listOf("a", "b", "c", "b"), requests)
            assertEquals(emptySet<String>(), viewModel.videosState.value.failedIds)
            assertEquals(
                mapOf("a" to "response-a", "b" to "response-b", "c" to "response-c"),
                viewModel.videosState.value.videosByRequestedId
                    .mapValues { it.value.id },
            )
        }

    @Test
    fun aVideoStillLoadingIsNotRequestedAgain() =
        runTest(dispatcher) {
            val requests = mutableListOf<String>()
            val response = CompletableDeferred<Video>()
            val viewModel =
                ContentVideosViewModel(
                    playlistsPage = { _, _ -> emptyList<VideoPlaylist>() to null },
                    videoDetail = { id ->
                        requests += id
                        response.await()
                    },
                )

            viewModel.loadVideos(listOf("a"))
            advanceUntilIdle()
            viewModel.loadVideos(listOf("a"))
            response.complete(video("response-a"))
            advanceUntilIdle()

            assertEquals(listOf("a"), requests)
            assertEquals(
                "response-a",
                viewModel.videosState.value.videosByRequestedId["a"]
                    ?.id,
            )
        }

    @Test
    fun aPlaylistRepeatedOnALaterPageIsListedOnceInTitleOrder() =
        runTest(dispatcher) {
            val viewModel =
                ContentVideosViewModel(
                    playlistsPage = { _, token ->
                        if (token == null) {
                            listOf(VideoPlaylist(id = 2, title = "Torah"), VideoPlaylist(id = 1, title = "Advent")) to
                                "p2"
                        } else {
                            listOf(VideoPlaylist(id = 2, title = "Torah")) to null
                        }
                    },
                    videoDetail = { error("unused") },
                )
            advanceUntilIdle()

            assertEquals(
                listOf(1, 2),
                viewModel.collectionsState.value.collections
                    .map { it.id },
            )
        }

    @Test
    fun aRepeatedPageTokenIsAnError() =
        runTest(dispatcher) {
            val viewModel =
                ContentVideosViewModel(
                    playlistsPage = { _, _ -> listOf(VideoPlaylist(id = 1)) to "same" },
                    videoDetail = { error("unused") },
                )
            advanceUntilIdle()

            assertEquals(true, viewModel.collectionsState.value.hasError)
            assertEquals(emptyList<VideoPlaylist>(), viewModel.collectionsState.value.collections)
        }

    @Test
    fun videoDetailsLoadAtMostFourAtATime() =
        runTest(dispatcher) {
            var running = 0
            var maxRunning = 0
            val gate = CompletableDeferred<Unit>()
            val viewModel =
                ContentVideosViewModel(
                    playlistsPage = { _, _ -> emptyList<VideoPlaylist>() to null },
                    videoDetail = { id ->
                        running++
                        maxRunning = maxOf(maxRunning, running)
                        gate.await()
                        running--
                        video("response-$id")
                    },
                )

            viewModel.loadVideos((1..20).map { "v$it" })
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(4, maxRunning)
            assertEquals(20, viewModel.videosState.value.videosByRequestedId.size)
        }

    @Test
    fun resultsForAPreviousLanguageAreDropped() =
        runTest(dispatcher) {
            val englishVideo = CompletableDeferred<Video>()
            val playlistLanguages = mutableListOf<String>()
            val viewModel =
                ContentVideosViewModel(
                    initialLanguageTag = "es",
                    playlistsPage = { languageTag, _ ->
                        playlistLanguages += languageTag
                        listOf(VideoPlaylist(id = 1, title = languageTag)) to null
                    },
                    videoDetail = { englishVideo.await() },
                )
            advanceUntilIdle()
            viewModel.setLanguage("en")
            viewModel.loadVideos(listOf("a"))
            advanceUntilIdle()
            viewModel.setLanguage("fr")
            englishVideo.complete(video("response-a"))
            advanceUntilIdle()

            assertEquals(listOf("es", "en", "fr"), playlistLanguages)
            assertEquals(
                listOf("fr"),
                viewModel.collectionsState.value.collections
                    .map { it.title },
            )
            assertEquals(ContentVideosState(), viewModel.videosState.value)
        }
}
