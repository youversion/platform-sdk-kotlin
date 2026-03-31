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

    private fun setNavigationContent(startDestination: String = "reader") {
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                composable("reader") { Text("Reader Screen") }
                composable("versions") { Text("Versions Screen") }
                composable("languages") { Text("Languages Screen") }
                composable("references") { Text("References Screen") }
                composable("fonts") { Text("Fonts Screen") }
            }
        }
    }

    // ----- Default Start Destination

    @Test
    fun `default start destination is reader`() {
        setNavigationContent()

        assertEquals("reader", navController.currentDestination?.route)
    }

    // ----- Reader Navigation

    @Test
    fun `navigating to versions route shows versions destination`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate("versions")
        }

        assertEquals("versions", navController.currentDestination?.route)
    }

    @Test
    fun `navigating to references route shows references destination`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate("references")
        }

        assertEquals("references", navController.currentDestination?.route)
    }

    @Test
    fun `navigating to fonts route shows fonts destination`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate("fonts")
        }

        assertEquals("fonts", navController.currentDestination?.route)
    }

    // ----- Versions Navigation

    @Test
    fun `navigating from versions to languages shows languages destination`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate("versions")
        }
        composeTestRule.runOnUiThread {
            navController.navigate("languages")
        }

        assertEquals("languages", navController.currentDestination?.route)
    }

    @Test
    fun `popping back stack from versions returns to reader`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate("versions")
        }
        assertEquals("versions", navController.currentDestination?.route)

        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }

        assertEquals("reader", navController.currentDestination?.route)
    }

    // ----- Languages Navigation

    @Test
    fun `popping back stack from languages returns to versions`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate("versions")
            navController.navigate("languages")
        }
        assertEquals("languages", navController.currentDestination?.route)

        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }

        assertEquals("versions", navController.currentDestination?.route)
    }

    // ----- References Navigation

    @Test
    fun `popping back stack from references returns to reader`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate("references")
        }
        assertEquals("references", navController.currentDestination?.route)

        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }

        assertEquals("reader", navController.currentDestination?.route)
    }

    // ----- Fonts Navigation

    @Test
    fun `popping back stack from fonts returns to reader`() {
        setNavigationContent()

        composeTestRule.runOnUiThread {
            navController.navigate("fonts")
        }
        assertEquals("fonts", navController.currentDestination?.route)

        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }

        assertEquals("reader", navController.currentDestination?.route)
    }

    // ----- BibleReaderDestination Routes

    @Test
    fun `navigation graph contains all five expected routes`() {
        setNavigationContent()

        val expectedRoutes = setOf("reader", "versions", "languages", "references", "fonts")
        val actualRoutes = navController.graph.map { it.route }.toSet()

        assertEquals(expectedRoutes, actualRoutes)
    }
}
