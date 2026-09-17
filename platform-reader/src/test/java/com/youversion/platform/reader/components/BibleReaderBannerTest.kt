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
                "We’re having difficulties with your connection. Please download a Bible version when you’re online.",
            ).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Offline").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
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
                "Your previously selected Bible version is unavailable. Please switch to another one.",
            ).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Version unavailable").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
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

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        assertTrue(isDismissed.value)
        composeTestRule
            .onNodeWithText(
                "Your previously selected Bible version is unavailable. Please switch to another one.",
            ).assertIsNotDisplayed()
        composeTestRule.onNodeWithContentDescription("Version unavailable").assertIsNotDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertIsNotDisplayed()
    }
}
