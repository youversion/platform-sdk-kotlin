package com.youversion.platform.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class BibleReaderNavigationTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: NavHostController

    private var switchToVersionCalledWith: Int? = null
    private var loadVersionsForLanguageCalledWith: String? = null
    private var headerSelectionChangeCalledWith: BibleReference? = null
    private var introSelectedBookCode: String? = null
    private var introSelectedPassageId: String? = null

    @Before
    fun resetTracking() {
        switchToVersionCalledWith = null
        loadVersionsForLanguageCalledWith = null
        headerSelectionChangeCalledWith = null
        introSelectedBookCode = null
        introSelectedPassageId = null
    }

    private fun setNavigationContent(startDestination: String = BibleReaderDestination.Reader.route) {
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                composable(BibleReaderDestination.Reader.route) { Text("Reader Screen") }
                composable(BibleReaderDestination.Versions.route) { Text("Versions Screen") }
                composable(BibleReaderDestination.Languages.route) { Text("Languages Screen") }
                composable(BibleReaderDestination.References.route) { Text("References Screen") }
                composable(BibleReaderDestination.Fonts.route) { Text("Fonts Screen") }
            }
        }
    }

    private fun setCallbackNavigationContent(bibleVersion: BibleVersion? = null) {
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = BibleReaderDestination.Reader.route,
            ) {
                composable(BibleReaderDestination.Reader.route) { Text("Reader Screen") }

                composable(BibleReaderDestination.Versions.route) {
                    Column {
                        Text("Versions Screen")
                        Button(
                            onClick = {
                                navController.popBackStack()
                                switchToVersionCalledWith = 1
                            },
                            modifier = Modifier.testTag("select-version"),
                        ) { Text("Select Version") }
                        Button(
                            onClick = {
                                navController.navigate(BibleReaderDestination.Languages.route)
                            },
                            modifier = Modifier.testTag("go-to-languages"),
                        ) { Text("Languages") }
                    }
                }

                composable(BibleReaderDestination.Languages.route) {
                    Column {
                        Text("Languages Screen")
                        Button(
                            onClick = {
                                loadVersionsForLanguageCalledWith = "es"
                                navController.popBackStack()
                            },
                            modifier = Modifier.testTag("select-language"),
                        ) { Text("Select Language") }
                    }
                }

                composable(BibleReaderDestination.References.route) {
                    bibleVersion?.let {
                        Column(modifier = Modifier.testTag("references-content")) {
                            Text("References Screen")
                            Button(
                                onClick = {
                                    val chapter = "3"
                                    val chapterNumber = chapter.toIntOrNull()
                                    if (chapterNumber == null) {
                                        introSelectedBookCode = "GEN"
                                        introSelectedPassageId = chapter
                                    } else {
                                        headerSelectionChangeCalledWith =
                                            BibleReference(
                                                versionId = 1,
                                                bookUSFM = "GEN",
                                                chapter = chapterNumber,
                                            )
                                    }
                                    navController.popBackStack()
                                },
                                modifier = Modifier.testTag("select-numeric-chapter"),
                            ) { Text("Select Chapter") }
                            Button(
                                onClick = {
                                    val chapter = "INTRO"
                                    val chapterNumber = chapter.toIntOrNull()
                                    if (chapterNumber == null) {
                                        introSelectedBookCode = "GEN"
                                        introSelectedPassageId = chapter
                                    } else {
                                        headerSelectionChangeCalledWith =
                                            BibleReference(
                                                versionId = 1,
                                                bookUSFM = "GEN",
                                                chapter = chapterNumber,
                                            )
                                    }
                                    navController.popBackStack()
                                },
                                modifier = Modifier.testTag("select-intro"),
                            ) { Text("Select Intro") }
                        }
                    }
                }

                composable(BibleReaderDestination.Fonts.route) { Text("Fonts Screen") }
            }
        }
    }

    // ----- Default Start Destination

    @Test
    fun `default start destination is reader`() {
        setNavigationContent()

        assertEquals(BibleReaderDestination.Reader.route, navController.currentDestination?.route)
    }

    // ----- Reader Navigation

    @Test
    fun `navigating to versions route shows versions destination`() {
        setNavigationContent()
        assertEquals(BibleReaderDestination.Reader.route, navController.currentDestination?.route)

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.Versions.route)
        }

        assertEquals(BibleReaderDestination.Versions.route, navController.currentDestination?.route)
    }

    @Test
    fun `navigating to references route shows references destination`() {
        setNavigationContent()
        assertEquals(BibleReaderDestination.Reader.route, navController.currentDestination?.route)

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.References.route)
        }

        assertEquals(BibleReaderDestination.References.route, navController.currentDestination?.route)
    }

    @Test
    fun `navigating to fonts route shows fonts destination`() {
        setNavigationContent()
        assertEquals(BibleReaderDestination.Reader.route, navController.currentDestination?.route)

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.Fonts.route)
        }

        assertEquals(BibleReaderDestination.Fonts.route, navController.currentDestination?.route)
    }

    // ----- Versions Navigation

    @Test
    fun `navigating from versions to languages shows languages destination`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.Versions.route)
        }
        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.Languages.route)
        }

        assertEquals(BibleReaderDestination.Languages.route, navController.currentDestination?.route)
    }

    @Test
    fun `popping back stack from versions returns to reader`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.Versions.route)
        }
        assertEquals(BibleReaderDestination.Versions.route, navController.currentDestination?.route)

        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }

        assertEquals(BibleReaderDestination.Reader.route, navController.currentDestination?.route)
    }

    @Test
    fun `versions onVersionSelect pops back stack and calls switchToVersion`() {
        setCallbackNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.Versions.route)
        }
        assertEquals(BibleReaderDestination.Versions.route, navController.currentDestination?.route)

        composeTestRule.onNodeWithTag("select-version").performClick()

        assertEquals(1, switchToVersionCalledWith)
        assertEquals(BibleReaderDestination.Reader.route, navController.currentDestination?.route)
    }

    // ----- Languages Navigation

    @Test
    fun `popping back stack from languages returns to versions`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.Versions.route)
            navController.navigate(BibleReaderDestination.Languages.route)
        }
        assertEquals(BibleReaderDestination.Languages.route, navController.currentDestination?.route)

        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }

        assertEquals(BibleReaderDestination.Versions.route, navController.currentDestination?.route)
    }

    @Test
    fun `languages onLanguageTagSelected calls loadVersionsForLanguage and pops back stack`() {
        setCallbackNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.Versions.route)
            navController.navigate(BibleReaderDestination.Languages.route)
        }
        assertEquals(BibleReaderDestination.Languages.route, navController.currentDestination?.route)

        composeTestRule.onNodeWithTag("select-language").performClick()

        assertEquals("es", loadVersionsForLanguageCalledWith)
        assertEquals(BibleReaderDestination.Versions.route, navController.currentDestination?.route)
    }

    // ----- References Navigation

    @Test
    fun `popping back stack from references returns to reader`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.References.route)
        }
        assertEquals(BibleReaderDestination.References.route, navController.currentDestination?.route)

        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }

        assertEquals(BibleReaderDestination.Reader.route, navController.currentDestination?.route)
    }

    @Test
    fun `references selection with numeric chapter calls onHeaderSelectionChange and pops back stack`() {
        setCallbackNavigationContent(bibleVersion = BibleVersion(id = 1))

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.References.route)
        }
        assertEquals(BibleReaderDestination.References.route, navController.currentDestination?.route)

        composeTestRule.onNodeWithTag("select-numeric-chapter").performClick()

        assertEquals(
            BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 3),
            headerSelectionChangeCalledWith,
        )
        assertNull(introSelectedBookCode)
        assertEquals(BibleReaderDestination.Reader.route, navController.currentDestination?.route)
    }

    @Test
    fun `references selection with non-numeric chapter calls onIntroSelected and pops back stack`() {
        setCallbackNavigationContent(bibleVersion = BibleVersion(id = 1))

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.References.route)
        }
        assertEquals(BibleReaderDestination.References.route, navController.currentDestination?.route)

        composeTestRule.onNodeWithTag("select-intro").performClick()

        assertEquals("GEN", introSelectedBookCode)
        assertEquals("INTRO", introSelectedPassageId)
        assertNull(headerSelectionChangeCalledWith)
        assertEquals(BibleReaderDestination.Reader.route, navController.currentDestination?.route)
    }

    @Test
    fun `references screen does not render content when bibleVersion is null`() {
        setCallbackNavigationContent(bibleVersion = null)

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.References.route)
        }
        assertEquals(BibleReaderDestination.References.route, navController.currentDestination?.route)

        composeTestRule.onNodeWithTag("references-content").assertDoesNotExist()
    }

    // ----- Fonts Navigation

    @Test
    fun `popping back stack from fonts returns to reader`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate(BibleReaderDestination.Fonts.route)
        }
        assertEquals(BibleReaderDestination.Fonts.route, navController.currentDestination?.route)

        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }

        assertEquals(BibleReaderDestination.Reader.route, navController.currentDestination?.route)
    }
}
