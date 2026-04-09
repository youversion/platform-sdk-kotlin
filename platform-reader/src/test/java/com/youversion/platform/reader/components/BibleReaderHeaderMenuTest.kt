package com.youversion.platform.reader.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleReaderHeaderMenuTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the BibleReaderHeaderMenu when dropdown menu is clicked`() {
        composeTestRule.setContent {
            BibleReaderHeaderDropdownMenu(
                isSignInProcessing = false,
                signedIn = false,
                onOpenMenu = {},
                onFontSettingsClick = {},
                onSignInClick = {},
                onSignOutClick = {},
            )
        }

        composeTestRule.onNodeWithText("Font & Settings").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertIsNotDisplayed()

        composeTestRule.onNodeWithContentDescription("Fonts & Settings").performClick()
        composeTestRule.onNodeWithText("Font & Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    }

    @Test
    fun `Font settings triggers the onFontSettingsClick`() {
        var fontSettingsClicked = false
        composeTestRule.setContent {
            BibleReaderHeaderDropdownMenu(
                isSignInProcessing = false,
                signedIn = false,
                onOpenMenu = {},
                onFontSettingsClick = { fontSettingsClicked = true },
                onSignInClick = {},
                onSignOutClick = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("Fonts & Settings").performClick()
        composeTestRule.onNodeWithText("Font & Settings").performClick()
        assertTrue(fontSettingsClicked)

        composeTestRule.onNodeWithText("Sign In").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Font & Settings").assertIsNotDisplayed()
    }

    @Test
    fun `Sign In triggers the onSignInClick`() {
        val signInClicked = mutableStateOf(false)
        composeTestRule.setContent {
            BibleReaderHeaderDropdownMenu(
                isSignInProcessing = false,
                signedIn = signInClicked.value,
                onOpenMenu = {},
                onFontSettingsClick = {},
                onSignInClick = { signInClicked.value = true },
                onSignOutClick = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("Fonts & Settings").performClick()
        composeTestRule.onNodeWithText("Sign In").performClick()
        assertTrue(signInClicked.value)

        composeTestRule.onNodeWithText("Font & Settings").assertIsNotDisplayed()
        composeTestRule.onNodeWithContentDescription("Fonts & Settings").performClick()

        composeTestRule.onNodeWithText("Font & Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign Out").assertIsDisplayed()
    }

    @Test
    fun `Sign Out triggers the onSignOutClick`() {
        val signOutClicked = mutableStateOf(false)
        composeTestRule.setContent {
            BibleReaderHeaderDropdownMenu(
                isSignInProcessing = false,
                signedIn = !signOutClicked.value,
                onOpenMenu = {},
                onFontSettingsClick = {},
                onSignInClick = {},
                onSignOutClick = { signOutClicked.value = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Fonts & Settings").performClick()
        composeTestRule.onNodeWithText("Sign Out").performClick()
        assertTrue(signOutClicked.value)

        composeTestRule.onNodeWithText("Font & Settings").assertIsNotDisplayed()
        composeTestRule.onNodeWithContentDescription("Fonts & Settings").performClick()

        composeTestRule.onNodeWithText("Font & Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    }

    @Test
    fun `Sign In and Sign Out menu items disabled when isSignInProcessing is true`() {
        val isSignedIn = mutableStateOf(false)
        val loading = mutableStateOf(true)
        composeTestRule.setContent {
            BibleReaderHeaderDropdownMenu(
                isSignInProcessing = loading.value,
                signedIn = isSignedIn.value,
                onOpenMenu = {},
                onFontSettingsClick = {},
                onSignInClick = { isSignedIn.value = true },
                onSignOutClick = { isSignedIn.value = false },
            )
        }

        composeTestRule.onNodeWithContentDescription("Fonts & Settings").performClick()
        composeTestRule.onNodeWithText("Sign In").assertIsNotEnabled()
        loading.value = false

        composeTestRule.onNodeWithText("Sign In").performClick()
        loading.value = true

        composeTestRule.onNodeWithContentDescription("Fonts & Settings").performClick()
        composeTestRule.onNodeWithText("Sign Out").assertIsNotEnabled()
        loading.value = false
        composeTestRule.onNodeWithText("Sign Out").assertIsEnabled()
    }
}
