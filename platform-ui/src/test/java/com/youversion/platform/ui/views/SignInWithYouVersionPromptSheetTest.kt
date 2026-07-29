package com.youversion.platform.ui.views

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SignInWithYouVersionPromptSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderSheet(
        appName: String = "Test App",
        appSignInMessage: String? = null,
        onSignIn: () -> Unit = {},
        onDismissRequest: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                SignInWithYouVersionPromptSheet(
                    appName = appName,
                    onSignIn = onSignIn,
                    onDismissRequest = onDismissRequest,
                    appSignInMessage = appSignInMessage,
                )
            }
        }
    }

    @Test
    fun `names the app that is asking`() {
        renderSheet(appName = "Test App")

        composeTestRule
            .onNodeWithText("Test App wants to connect", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `shows the host app's own message when it has one`() {
        renderSheet(appSignInMessage = "Save your highlights across devices")

        composeTestRule
            .onNodeWithText("Save your highlights across devices")
            .assertIsDisplayed()
    }

    @Test
    fun `still explains itself when the host app has no message of its own`() {
        renderSheet(appSignInMessage = null)

        composeTestRule
            .onNodeWithText("Test App wants to connect", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Yes Please").assertIsDisplayed()
    }

    @Test
    fun `omits the host app's message when it is blank`() {
        renderSheet(appSignInMessage = "   ")

        composeTestRule.onNodeWithText("   ").assertDoesNotExist()
    }

    @Test
    fun `offers to proceed and to decline`() {
        renderSheet()

        composeTestRule.onNodeWithText("Yes Please").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Thanks").assertIsDisplayed()
    }

    @Test
    fun `accepting calls onSignIn`() {
        var isSigningIn = false

        renderSheet(onSignIn = { isSigningIn = true })
        composeTestRule.onNodeWithText("Yes Please").performClick()

        assertTrue(isSigningIn)
    }

    @Test
    fun `declining calls onDismissRequest`() {
        var isDismissed = false

        renderSheet(onDismissRequest = { isDismissed = true })
        composeTestRule.onNodeWithText("No Thanks").performClick()

        assertTrue(isDismissed)
    }
}
