package com.youversion.platform.reader

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.reader.di.PlatformReaderKoinModule
import com.youversion.platform.reader.screens.bible.BibleScreen
import com.youversion.platform.reader.screens.fonts.FontsScreen
import com.youversion.platform.reader.screens.references.ReferencesScreen
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.views.versions.LanguagesScreen
import com.youversion.platform.ui.views.versions.VersionsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.module.rememberKoinModules
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

/**
 * A complete Bible reader.
 *
 * The sign-in prompt a signed-out reader sees names the host app and shows its reason for asking, both of which come
 * from [YouVersionPlatformConfiguration.appName] and [YouVersionPlatformConfiguration.signInPromptMessage].
 *
 * @param bibleReference The reference to open. When null, the reader restores the last-read reference.
 * @param fontDefinitionProvider The fonts the host app offers in the reader's font settings.
 * @param bottomBar The host app's own bottom bar, drawn below the reader.
 */
@OptIn(KoinExperimentalAPI::class)
@Composable
fun BibleReader(
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
        val versionViewModel = bibleReaderViewModel.bibleVersionsViewModel

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
                        viewModel = versionViewModel,
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
                                val chapterNumber = chapter.toIntOrNull()
                                if (chapterNumber == null) {
                                    bibleReaderViewModel.onIntroSelected(bookCode, chapter)
                                } else {
                                    BibleReference(
                                        versionId = versionId,
                                        bookUSFM = bookCode,
                                        chapter = chapter.toInt(),
                                    ).also { bibleReaderViewModel.onHeaderSelectionChange(it) }
                                }
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

/**
 * A complete Bible reader, taking the sign-in prompt's copy as parameters.
 *
 * @param appName The name of the host app, shown in the sign-in prompt.
 * @param appSignInMessage The host app's own reason for asking the reader to sign in.
 * @param bibleReference The reference to open. When null, the reader restores the last-read reference.
 * @param fontDefinitionProvider The fonts the host app offers in the reader's font settings.
 * @param bottomBar The host app's own bottom bar, drawn below the reader.
 */
@Deprecated("Pass appName and signInPromptMessage to YouVersionPlatformConfiguration.configure() instead.")
@Composable
fun BibleReader(
    appName: String,
    appSignInMessage: String,
    bibleReference: BibleReference? = null,
    fontDefinitionProvider: FontDefinitionProvider? = null,
    bottomBar: @Composable () -> Unit = {},
) {
    LaunchedEffect(appName, appSignInMessage) {
        YouVersionPlatformConfiguration.configureSignIn(
            appName = appName,
            signInPromptMessage = appSignInMessage,
        )
    }

    BibleReader(
        bibleReference = bibleReference,
        fontDefinitionProvider = fontDefinitionProvider,
        bottomBar = bottomBar,
    )
}

internal sealed class BibleReaderDestination(
    val route: String,
) {
    data object Reader : BibleReaderDestination("reader")

    data object Versions : BibleReaderDestination("versions")

    data object Languages : BibleReaderDestination("languages")

    data object References : BibleReaderDestination("references")

    data object Fonts : BibleReaderDestination("fonts")
}
