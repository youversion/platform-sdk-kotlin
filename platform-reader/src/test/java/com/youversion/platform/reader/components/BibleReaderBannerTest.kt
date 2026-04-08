package com.youversion.platform.reader.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
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
class BibleReaderBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows offline banner`() {
        composeTestRule.setContent {
            BibleReaderBanner(
                bannerType = BibleReaderBannerType.OFFLINE,
                isVisible = true,
                onDismiss = {},
            )
        }

        composeTestRule
            .onNodeWithText(
                "You've lost your internet connection. Re-connect and download a Bible version to proceed offline.",
            ).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Offline Icon").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Dismiss").assertIsDisplayed()
    }

    @Test
    fun `shows version unavailable banner`() {
        composeTestRule.setContent {
            BibleReaderBanner(
                bannerType = BibleReaderBannerType.VERSION_UNAVAILABLE,
                isVisible = true,
                onDismiss = {},
            )
        }

        composeTestRule
            .onNodeWithText(
                "Your previously selected Bible version is unavailable. Another similar Bible version has been " +
                    "selected for you.",
            ).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Version Unavailable Icon").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Dismiss").assertIsDisplayed()
    }

    @Test
    fun `triggers the onDismiss and is no longer visible`() {
        val isDismissed = mutableStateOf(false)
        composeTestRule.setContent {
            BibleReaderBanner(
                bannerType = BibleReaderBannerType.VERSION_UNAVAILABLE,
                isVisible = !isDismissed.value,
                onDismiss = { isDismissed.value = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Dismiss").performClick()
        composeTestRule.waitForIdle()

        assertTrue(isDismissed.value)
        composeTestRule
            .onNodeWithText(
                "Your previously selected Bible version is unavailable. Another similar Bible version has been " +
                    "selected for you.",
            ).assertIsNotDisplayed()
        composeTestRule.onNodeWithContentDescription("Version Unavailable Icon").assertIsNotDisplayed()
        composeTestRule.onNodeWithContentDescription("Dismiss").assertIsNotDisplayed()
    }
}
