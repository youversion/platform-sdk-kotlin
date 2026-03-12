package com.youversion.platform.ui.views.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class BibleAppLogoTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Bible App text is displayed`() {
        composeTestRule.setContent {
            BibleAppLogo()
        }
        composeTestRule.onNodeWithText("Bible App").assertIsDisplayed()
    }

    @Test
    fun `YouVersion logo is displayed`() {
        composeTestRule.setContent {
            BibleAppLogo()
        }
        composeTestRule.onNodeWithContentDescription("YouVersion Logo").assertIsDisplayed()
    }
}
