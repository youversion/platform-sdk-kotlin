package com.youversion.platform.reader

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class BibleReaderNavigationTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: NavHostController

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
