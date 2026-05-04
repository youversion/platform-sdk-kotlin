package com.youversion.platform.ui.views.versions

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.youversion.platform.core.bibles.models.BibleVersion

@Composable
fun BibleVersionsStack(
    viewModel: BibleVersionsViewModel,
    onDismiss: () -> Unit,
    onVersionSelect: (BibleVersion) -> Unit,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Destination.Versions.route) {
        composable(route = Destination.Versions.route) {
            VersionsScreen(
                viewModel = viewModel,
                onBackClick = onDismiss,
                onLanguagesClick = { navController.navigate(Destination.Languages.route) },
                onVersionSelect = onVersionSelect,
            )
        }

        composable(route = Destination.Languages.route) {
            LanguagesScreen(
                viewModel = viewModel,
                onBackClick = navController::popBackStack,
                onLanguageTagSelected =
                    { languageTag ->
                        viewModel.loadVersionsForLanguage(languageTag)
                        navController.popBackStack()
                    },
            )
        }
    }
}

private sealed class Destination(
    val route: String,
) {
    data object Versions : Destination("versions")

    data object Languages : Destination("languages")
}
