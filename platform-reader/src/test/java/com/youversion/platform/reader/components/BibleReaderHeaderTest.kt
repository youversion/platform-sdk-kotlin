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

    @Test
    fun `renders header with version abbreviation`() {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleReaderHeader(
                    isSignInProcessing = false,
                    signedIn = true,
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
                    versionAbbreviation = "NIV",
                    onVersionClick = {},
                    onOpenHeaderMenu = {},
                    onFontSettingsClick = {},
                    onSignInClick = {},
                    onSignOutClick = {},
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Fonts & Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Language").assertIsDisplayed()
        composeTestRule.onNodeWithText("NIV").assertIsDisplayed()
    }

    @Test
    fun `clicking version button triggers onVersionClick`() {
        var onVersionClicked = false
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleReaderHeader(
                    isSignInProcessing = false,
                    signedIn = true,
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
                    versionAbbreviation = "NIV",
                    onVersionClick = { onVersionClicked = true },
                    onOpenHeaderMenu = {},
                    onFontSettingsClick = {},
                    onSignInClick = {},
                    onSignOutClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("NIV").performClick()
        assertTrue(onVersionClicked)
    }
}
