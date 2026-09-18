package com.youversion.platform.reader.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class BibleReaderHeaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /** Renders the header, defaulting everything a given test does not care about. */
    private fun renderHeader(
        onVersionClick: () -> Unit = {},
        onSearchClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleReaderHeader(
                    isSignInProcessing = false,
                    signedIn = true,
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
                    versionAbbreviation = "NIV",
                    onVersionClick = onVersionClick,
                    onSearchClick = onSearchClick,
                    onOpenHeaderMenu = {},
                    onFontSettingsClick = {},
                    onSignInClick = {},
                    onSignOutClick = {},
                )
            }
        }
    }

    @Test
    fun `renders header with version abbreviation`() {
        renderHeader()

        composeTestRule.onNodeWithContentDescription("Font & Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Language").assertIsDisplayed()
        composeTestRule.onNodeWithText("NIV").assertIsDisplayed()
    }

    @Test
    fun `clicking version button triggers onVersionClick`() {
        var onVersionClicked = false
        renderHeader(onVersionClick = { onVersionClicked = true })

        composeTestRule.onNodeWithText("NIV").performClick()

        assertTrue(onVersionClicked)
    }

    @Test
    fun `renders a labelled search button`() {
        renderHeader()

        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
    }

    @Test
    fun `clicking search button triggers onSearchClick`() {
        var onSearchClicked = false
        renderHeader(onSearchClick = { onSearchClicked = true })

        composeTestRule.onNodeWithContentDescription("Search").performClick()

        assertTrue(onSearchClicked)
    }
}
