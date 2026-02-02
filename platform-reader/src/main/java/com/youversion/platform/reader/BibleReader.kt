package com.youversion.platform.reader

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.foundation.PlatformKoinGraph
import com.youversion.platform.reader.di.PlatformReaderKoinModule
import com.youversion.platform.reader.screens.bible.BibleScreen
import com.youversion.platform.reader.screens.fonts.FontsScreen
import com.youversion.platform.reader.screens.languages.LanguagesScreen
import com.youversion.platform.reader.screens.references.ReferencesScreen
import com.youversion.platform.reader.screens.versions.VersionsScreen
import com.youversion.platform.reader.screens.versions.VersionsViewModel
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import com.youversion.platform.reader.theme.FontDefinitionProvider
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.module.rememberKoinModules
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

@OptIn(KoinExperimentalAPI::class)
@Composable
fun BibleReader(
    appName: String,
    appSignInMessage: String,
    bibleReference: BibleReference? = null,
    fontDefinitionProvider: FontDefinitionProvider? = null,
    bottomBar: @Composable () -> Unit = {},
) {
    KoinIsolatedContext(
        context = PlatformKoinGraph.koinApplication,
    ) {
        rememberKoinModules { listOf(PlatformReaderKoinModule) }

        val bibleReaderViewModel: BibleReaderViewModel =
            koinViewModel { parametersOf(bibleReference, fontDefinitionProvider) }
        val versionViewModel: VersionsViewModel = koinViewModel()

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
                        viewModel = bibleReaderViewModel,
                        appName = appName,
                        appSignInMessage = appSignInMessage,
                        bottomBar = bottomBar,
                        onReferencesClick = {
                            onDestinationClick(BibleReaderDestination.References)
                        },
                        onVersionsClick = {
                            onDestinationClick(BibleReaderDestination.Versions)
                        },
                        onFontsClick = {
                            onDestinationClick(BibleReaderDestination.Fonts)
                        },
                    )
                }

                composable(
                    route = BibleReaderDestination.Versions.route,
                ) {
                    VersionsScreen(
                        viewModel = versionViewModel,
                        onBackClick = navController::popBackStack,
                        onLanguagesClick = { onDestinationClick(BibleReaderDestination.Languages) },
                        onVersionSelect = { selectedVersion ->
                            navController.popBackStack()
                            bibleReaderViewModel.switchToVersion(selectedVersion.id)
                        },
                    )
                }

                composable(
                    route = BibleReaderDestination.Languages.route,
                ) {
                    LanguagesScreen(
                        bibleVersion = bibleReaderViewModel.bibleVersion,
                        onBackClick = navController::popBackStack,
                        onLanguageTagSelected = { languageTag ->
                            versionViewModel.loadVersionsForLanguage(languageTag)
                            navController.popBackStack()
                        },
                    )
                }
                composable(
                    route = BibleReaderDestination.References.route,
                ) {
                    bibleReaderViewModel.bibleVersion?.let {
                        ReferencesScreen(
                            bibleVersion = it,
                            bibleReference = bibleReaderViewModel.bibleReference,
                            onSelectionClick = { versionId, bookCode, chapter ->
                                BibleReference(
                                    versionId = versionId,
                                    bookUSFM = bookCode,
                                    chapter = chapter.toInt(),
                                ).also { bibleReaderViewModel.onHeaderSelectionChange(it) }
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
                        viewModel = bibleReaderViewModel,
                        onBackClick = navController::popBackStack,
                    )
                }
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
