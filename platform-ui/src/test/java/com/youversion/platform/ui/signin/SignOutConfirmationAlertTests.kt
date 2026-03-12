package com.youversion.platform.ui.signin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SignOutConfirmationAlertTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `dialog is displayed`() {
        composeTestRule.setContent {
            SignOutConfirmationAlert(onDismissRequest = {}, onConfirm = {})
        }
        // "Sign Out" appears as both the title and the confirm button text
        composeTestRule.onAllNodesWithText("Sign Out")[0].assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "Are you sure you want to sign out from YouVersion? You will need to sign in again to access your highlights.",
            ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun `confirm button invokes onConfirm`() {
        var onConfirmCalled = false
        composeTestRule.setContent {
            SignOutConfirmationAlert(onDismissRequest = {}, onConfirm = { onConfirmCalled = true })
        }
        // Index 1 is the confirm button (index 0 is the title text)
        composeTestRule.onAllNodesWithText("Sign Out")[1].performClick()
        assertTrue(onConfirmCalled)
    }

    @Test
    fun `cancel button invokes onDismissRequest`() {
        var onDismissRequestCalled = false
        composeTestRule.setContent {
            SignOutConfirmationAlert(
                onDismissRequest = { onDismissRequestCalled = true },
                onConfirm = {},
            )
        }
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue(onDismissRequestCalled)
    }
}
