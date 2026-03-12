package com.youversion.platform.ui.signin

import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SignInErrorAlertTests {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `dialog is displayed`() {
        composeTestRule.setContent {
            SignInErrorAlert(onDismissRequest = {}, onConfirm = {})
        }
        composeTestRule.onNodeWithText("Sign-In Failed").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "We were unable to sign you in at this time. Please try again.",
            ).assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun `confirm button invokes onConfirm`() {
        var onConfirmCalled = false
        composeTestRule.setContent {
            SignInErrorAlert(onDismissRequest = {}, onConfirm = { onConfirmCalled = true })
        }
        composeTestRule.onNodeWithText("OK").performClick()
        assertTrue(onConfirmCalled)
    }

    @Test
    fun `back press invokes onDismissRequest`() {
        var onDismissRequestCalled = false
        composeTestRule.setContent {
            SignInErrorAlert(onDismissRequest = { onDismissRequestCalled = true }, onConfirm = {})
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread {
            val dialog = ShadowDialog.getLatestDialog() as? ComponentDialog
            dialog?.onBackPressedDispatcher?.onBackPressed()
        }
        assertTrue(onDismissRequestCalled)
    }
}
