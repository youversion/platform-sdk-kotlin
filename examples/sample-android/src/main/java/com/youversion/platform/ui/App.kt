package com.youversion.platform.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.verseimages.models.VerseImage
import com.youversion.platform.core.videos.models.VideoPlaylist
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.content.ContentCollectionDetail
import com.youversion.platform.ui.content.ContentImageDetail
import com.youversion.platform.ui.content.ContentImagesViewModel
import com.youversion.platform.ui.content.ContentLanguageViewModel
import com.youversion.platform.ui.content.ContentVideoDetail
import com.youversion.platform.ui.content.ContentVideosViewModel
import com.youversion.platform.ui.content.displayTitle
import com.youversion.platform.ui.examples.BibleCardExample
import com.youversion.platform.ui.examples.NavigateExample
import com.youversion.platform.ui.examples.VerseMarkersExample
import com.youversion.platform.ui.examples.VotdExample
import com.youversion.platform.ui.tabs.BibleReaderViewTab
import com.youversion.platform.ui.tabs.ContentViewTab
import com.youversion.platform.ui.tabs.ExamplesViewTab
import com.youversion.platform.ui.tabs.ProfileViewTab
import kotlinx.coroutines.CancellationException

@Composable
fun App() {
    val navController = rememberNavController()
    val onDestinationClick: (SampleDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    val onBack: () -> Unit = { navController.navigateUp() }
    var readerReference by remember { mutableStateOf<BibleReference?>(null) }
    val onReadInBible: (BibleReference) -> Unit = { reference ->
        readerReference = reference
        navController.popBackStack(SampleDestination.Reader.route, inclusive = false, saveState = true)
        navController.navigate(SampleDestination.Reader.route) {
            popUpTo(SampleDestination.Reader.route) {
                inclusive = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = SampleDestination.Reader.route,
    ) {
        composable(
            route = SampleDestination.Reader.route,
        ) {
            BibleReaderViewTab(
                bibleReference = readerReference,
                onDestinationClick = onDestinationClick,
            )
        }

        navigation(
            route = SampleDestination.Content.route,
            startDestination = SampleDestination.ContentImages.route,
        ) {
            composable(
                route = SampleDestination.ContentImages.route,
            ) { entry ->
                val languageViewModel = contentLanguageViewModel(navController, entry)
                val languageTag by languageViewModel.languageTag.collectAsStateWithLifecycle()
                val viewModel = contentImagesViewModel(navController, entry)
                val videosViewModel = contentVideosViewModel(navController, entry)
                val collectionsState by videosViewModel.collectionsState.collectAsStateWithLifecycle()
                val videosState by videosViewModel.videosState.collectAsStateWithLifecycle()
                LaunchedEffect(languageTag) {
                    viewModel.setLanguage(languageTag)
                    videosViewModel.setLanguage(languageTag)
                }
                ContentViewTab(
                    languageTag = languageTag,
                    onLanguageSelect = languageViewModel::select,
                    collectionsState = collectionsState,
                    videosState = videosState,
                    onRetryCollections = videosViewModel::retryCollections,
                    onLoadVideos = videosViewModel::loadVideos,
                    viewModel = viewModel,
                    onDestinationClick = onDestinationClick,
                    onCollectionClick = { collection ->
                        collection.id?.let { navController.navigate(SampleDestination.ContentCollection.route(it)) }
                    },
                    onImageClick = { image ->
                        navController.navigate(SampleDestination.ContentImage.route(image.id))
                    },
                )
            }

            composable(
                route = SampleDestination.ContentImage.route,
                arguments =
                    listOf(
                        navArgument(SampleDestination.ContentImage.IMAGE_ID_ARGUMENT) { type = NavType.StringType },
                    ),
            ) { entry ->
                val viewModel = contentImagesViewModel(navController, entry)
                val state by viewModel.state.collectAsStateWithLifecycle()
                val imageId =
                    requireNotNull(entry.arguments?.getString(SampleDestination.ContentImage.IMAGE_ID_ARGUMENT))
                var image by remember(imageId) { mutableStateOf<VerseImage?>(null) }
                var hasError by remember(imageId) { mutableStateOf(false) }
                var retryCount by remember(imageId) { mutableIntStateOf(0) }
                LaunchedEffect(imageId, retryCount) {
                    hasError = false
                    try {
                        image = viewModel.image(imageId)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        hasError = true
                    }
                }
                val loadedImage = image
                if (loadedImage == null) {
                    LoadingOrRetry(hasError, "The image didn’t load.", onRetry = { retryCount++ })
                } else {
                    ContentImageDetail(
                        image = loadedImage,
                        languageTag = viewModel.languageTag,
                        bibleVersion = state.bibleVersion,
                        onBack = onBack,
                        onDestinationClick = onDestinationClick,
                        onReadInBible = onReadInBible,
                    )
                }
            }

            composable(
                route = SampleDestination.ContentCollection.route,
                arguments =
                    listOf(
                        navArgument(SampleDestination.ContentCollection.COLLECTION_ID_ARGUMENT) {
                            type = NavType.IntType
                        },
                    ),
            ) { entry ->
                val viewModel = contentVideosViewModel(navController, entry)
                val videosState by viewModel.videosState.collectAsStateWithLifecycle()
                val collectionId =
                    requireNotNull(entry.arguments?.getInt(SampleDestination.ContentCollection.COLLECTION_ID_ARGUMENT))
                val collection = rememberCollection(viewModel, collectionId)
                val loadedCollection = collection.value
                if (loadedCollection == null) {
                    LoadingOrRetry(collection.hasError, "The collection didn’t load.", onRetry = collection.retry)
                } else {
                    ContentCollectionDetail(
                        collection = loadedCollection,
                        videosState = videosState,
                        onLoadVideos = viewModel::loadVideos,
                        onBack = onBack,
                        onDestinationClick = onDestinationClick,
                        onVideoClick = { videoId ->
                            navController.navigate(SampleDestination.ContentVideo.route(collectionId, videoId))
                        },
                    )
                }
            }

            composable(
                route = SampleDestination.ContentVideo.route,
                arguments =
                    listOf(
                        navArgument(SampleDestination.ContentVideo.COLLECTION_ID_ARGUMENT) { type = NavType.IntType },
                        navArgument(SampleDestination.ContentVideo.VIDEO_ID_ARGUMENT) { type = NavType.StringType },
                    ),
            ) { entry ->
                val viewModel = contentVideosViewModel(navController, entry)
                val videosState by viewModel.videosState.collectAsStateWithLifecycle()
                val collectionId =
                    requireNotNull(entry.arguments?.getInt(SampleDestination.ContentVideo.COLLECTION_ID_ARGUMENT))
                val videoId =
                    requireNotNull(entry.arguments?.getString(SampleDestination.ContentVideo.VIDEO_ID_ARGUMENT))
                val collection = rememberCollection(viewModel, collectionId)
                LaunchedEffect(videoId) { viewModel.loadVideos(listOf(videoId)) }
                val video = videosState.videosByRequestedId[videoId]
                if (video == null) {
                    LoadingOrRetry(
                        hasError = videoId in videosState.failedIds,
                        message = "The video didn’t load.",
                        onRetry = { viewModel.loadVideos(listOf(videoId)) },
                    )
                } else {
                    ContentVideoDetail(
                        video = video,
                        collectionTitle = collection.value?.displayTitle,
                        onBack = onBack,
                        onDestinationClick = onDestinationClick,
                    )
                }
            }
        }

        navigation(
            route = SampleDestination.Examples.route,
            startDestination = SampleDestination.ExamplesList.route,
        ) {
            composable(
                route = SampleDestination.ExamplesList.route,
            ) {
                ExamplesViewTab(
                    onDestinationClick = onDestinationClick,
                    onExampleClick = { example -> navController.navigate(example.route) },
                )
            }

            composable(
                route = SampleDestination.Navigate.route,
            ) {
                NavigateExample(
                    onDestinationClick = onDestinationClick,
                    onBack = onBack,
                    onReadInBible = onReadInBible,
                )
            }

            composable(
                route = SampleDestination.Votd.route,
            ) {
                VotdExample(
                    onDestinationClick = onDestinationClick,
                    onBack = onBack,
                )
            }

            composable(
                route = SampleDestination.BibleCard.route,
            ) {
                BibleCardExample(
                    onDestinationClick = onDestinationClick,
                    onBack = onBack,
                )
            }

            composable(
                route = SampleDestination.VerseMarkers.route,
            ) {
                VerseMarkersExample(
                    onDestinationClick = onDestinationClick,
                    onBack = onBack,
                )
            }
        }

        composable(
            route = SampleDestination.Profile.route,
        ) {
            ProfileViewTab(onDestinationClick = onDestinationClick)
        }
    }
}

/** The image library's view model, shared by every screen in the Content tab. */
@Composable
private fun contentImagesViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): ContentImagesViewModel {
    val contentEntry = remember(entry) { navController.getBackStackEntry(SampleDestination.Content.route) }
    val languageTag = contentLanguageViewModel(navController, entry).languageTag.value
    return viewModel(
        contentEntry,
    ) { ContentImagesViewModel(createSavedStateHandle(), initialLanguageTag = languageTag) }
}

/** The video library's view model, shared by every screen in the Content tab. */
@Composable
private fun contentVideosViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): ContentVideosViewModel {
    val contentEntry = remember(entry) { navController.getBackStackEntry(SampleDestination.Content.route) }
    val languageTag = contentLanguageViewModel(navController, entry).languageTag.value
    return viewModel(contentEntry) { ContentVideosViewModel(initialLanguageTag = languageTag) }
}

/** The Content tab's saved content language, shared by both libraries. */
@Composable
private fun contentLanguageViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): ContentLanguageViewModel {
    val contentEntry = remember(entry) { navController.getBackStackEntry(SampleDestination.Content.route) }
    return viewModel(contentEntry) { ContentLanguageViewModel(createSavedStateHandle()) }
}

/**
 * A collection that loads by id, for a screen restored after process death.
 *
 * @property value The collection, or `null` until it loads.
 * @property hasError Whether it failed to load.
 * @property retry Loads it again.
 */
private class LoadedCollection(
    val value: VideoPlaylist?,
    val hasError: Boolean,
    val retry: () -> Unit,
)

@Composable
private fun rememberCollection(
    viewModel: ContentVideosViewModel,
    collectionId: Int,
): LoadedCollection {
    var collection by remember(collectionId) { mutableStateOf<VideoPlaylist?>(null) }
    var hasError by remember(collectionId) { mutableStateOf(false) }
    var retryCount by remember(collectionId) { mutableIntStateOf(0) }
    LaunchedEffect(collectionId, retryCount) {
        hasError = false
        try {
            collection = viewModel.collection(collectionId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            hasError = true
        }
    }
    return LoadedCollection(collection, hasError, retry = { retryCount++ })
}

/** A spinner, or [message] with a Retry button when [hasError]. */
@Composable
private fun LoadingOrRetry(
    hasError: Boolean,
    message: String,
    onRetry: () -> Unit,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if (hasError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(message)
                OutlinedButton(onClick = onRetry) { Text("Retry") }
            }
        } else {
            CircularProgressIndicator()
        }
    }
}
