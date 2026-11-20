package com.youversion.platform.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.content.ContentImageDetail
import com.youversion.platform.ui.content.ContentImagesViewModel
import com.youversion.platform.ui.examples.BibleCardExample
import com.youversion.platform.ui.examples.NavigateExample
import com.youversion.platform.ui.examples.VotdExample
import com.youversion.platform.ui.tabs.BibleReaderViewTab
import com.youversion.platform.ui.tabs.ContentViewTab
import com.youversion.platform.ui.tabs.ExamplesViewTab
import com.youversion.platform.ui.tabs.ProfileViewTab

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
                val viewModel = contentImagesViewModel(navController, entry)
                ContentViewTab(
                    viewModel = viewModel,
                    onDestinationClick = onDestinationClick,
                    onImageClick = { image ->
                        viewModel.openedImage = image
                        navController.navigate(SampleDestination.ContentImage.route)
                    },
                )
            }

            composable(
                route = SampleDestination.ContentImage.route,
            ) { entry ->
                val viewModel = contentImagesViewModel(navController, entry)
                val state by viewModel.state.collectAsStateWithLifecycle()
                val image = viewModel.openedImage
                if (image == null) {
                    LaunchedEffect(Unit) { navController.navigateUp() }
                } else {
                    ContentImageDetail(
                        image = image,
                        bibleVersion = state.bibleVersion,
                        onBack = onBack,
                        onDestinationClick = onDestinationClick,
                        onReadInBible = onReadInBible,
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
    return viewModel(contentEntry) { ContentImagesViewModel() }
}
