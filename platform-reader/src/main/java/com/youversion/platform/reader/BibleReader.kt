package com.youversion.platform.reader

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.reader.screens.bible.BibleScreen
import com.youversion.platform.reader.screens.fonts.FontsScreen
import com.youversion.platform.reader.screens.references.ReferencesScreen
import com.youversion.platform.reader.screens.versions.LanguagesScreen
import com.youversion.platform.reader.screens.versions.VersionsScreen
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import com.youversion.platform.reader.theme.FontDefinitionProvider

@Composable
fun BibleReader(
    appName: String,
    appSignInMessage: String,
    bibleReference: BibleReference? = null,
    fontDefinitionProvider: FontDefinitionProvider? = null,
    bottomBar: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: BibleReaderViewModel =
        viewModel(factory = BibleReaderViewModel.factory(context, bibleReference, fontDefinitionProvider))

    val navController = rememberNavController()
    val onDestinationClick: (BibleReaderDestination) -> Unit = { destination ->
        navController.navigate(destination.route)
    }

    BibleReaderMaterialTheme {
        NavHost(
            navController = navController,
            startDestination = BibleReaderDestination.Reader.route,
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
        ) {
            composable(
                route = BibleReaderDestination.Reader.route,
            ) {
                BibleScreen(
                    viewModel = viewModel,
                    appName = appName,
                    appSignInMessage = appSignInMessage,
                    bottomBar = bottomBar,
                    onReferencesClick = { onDestinationClick(BibleReaderDestination.References) },
                    onVersionsClick = { onDestinationClick(BibleReaderDestination.Versions) },
                    onFontsClick = { onDestinationClick(BibleReaderDestination.Fonts) },
                )
            }

            composable(
                route = BibleReaderDestination.Versions.route,
            ) {
                VersionsScreen(
                    bibleVersion = viewModel.bibleVersion,
                    onBackClick = navController::popBackStack,
                    onLanguagesClick = { onDestinationClick(BibleReaderDestination.Languages) },
                    onVersionSelect = { selectedVersion ->
                        navController.popBackStack()
                        viewModel.switchToVersion(selectedVersion.id)
                    },
                )
            }

            composable(
                route = BibleReaderDestination.Languages.route,
            ) {
                LanguagesScreen(
                    onBackClick = navController::popBackStack,
                )
            }
            composable(
                route = BibleReaderDestination.References.route,
            ) {
                viewModel.bibleVersion?.let {
                    ReferencesScreen(
                        bibleVersion = it,
                        bibleReference = viewModel.bibleReference,
                        onSelectionClick = { versionId, bookCode, chapter ->
                            BibleReference(
                                versionId = versionId,
                                bookUSFM = bookCode,
                                chapter = chapter.toInt(),
                            ).also { viewModel.onHeaderSelectionChange(it) }
                            navController.popBackStack()
                        },
                        onBackClick = navController::popBackStack,
                    )
                }
            }
            composable(
                route = BibleReaderDestination.Fonts.route,
            ) {
                FontsScreen(
                    viewModel = viewModel,
                    onBackClick = navController::popBackStack,
                )
            }
        }
    }
}

private sealed class BibleReaderDestination(
    val route: String,
) {
    data object Reader : BibleReaderDestination("reader")

    data object Versions : BibleReaderDestination("versions")

    data object Languages : BibleReaderDestination("languages")

    data object References : BibleReaderDestination("references")

    data object Fonts : BibleReaderDestination("fonts")
}
