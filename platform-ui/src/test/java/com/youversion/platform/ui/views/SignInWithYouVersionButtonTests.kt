package com.youversion.platform.ui.views

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.users.api.UsersApi
import com.youversion.platform.ui.signin.YouVersionAuthentication
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class SignInWithYouVersionButtonTests {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var mockUsersApi: UsersApi

    private fun assertModeRenders(
        mode: SignInWithYouVersionButtonMode,
        assertModeSpecificContent: (fullLabel: String, compactLabel: String) -> Unit,
    ) {
        val fullLabel = "Sign in with YouVersion"
        val compactLabel = "Sign in"
        val iconLabel = "Bible Logo"

        composeTestRule.setContent {
            SignInWithYouVersionButton(
                permissions = { emptySet() },
                mode = mode,
                stroked = false,
                dark = true,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(iconLabel).assertIsDisplayed()
        assertModeSpecificContent(fullLabel, compactLabel)
    }

    @Before
    fun setUp() {
        mockkObject(YouVersionPlatformConfiguration)
        mockkObject(YouVersionApi)
        mockkObject(YouVersionAuthentication)

        every { YouVersionPlatformConfiguration.configState } returns MutableStateFlow(null)

        mockUsersApi = mockk(relaxed = true)
        every { YouVersionApi.users } returns mockUsersApi
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `renders FULL mode`() {
        assertModeRenders(SignInWithYouVersionButtonMode.FULL) { fullLabel, compactLabel ->
            composeTestRule.onNodeWithText(fullLabel).assertIsDisplayed()
            composeTestRule.onNodeWithText(compactLabel).assertDoesNotExist()
        }
    }

    @Test
    fun `renders COMPACT mode`() {
        assertModeRenders(SignInWithYouVersionButtonMode.COMPACT) { fullLabel, compactLabel ->
            composeTestRule.onNodeWithText(compactLabel).assertIsDisplayed()
            composeTestRule.onNodeWithText(fullLabel).assertDoesNotExist()
        }
    }

    @Test
    fun `renders ICON_ONLY mode`() {
        assertModeRenders(SignInWithYouVersionButtonMode.ICON_ONLY) { fullLabel, compactLabel ->
            composeTestRule.onNodeWithText(fullLabel).assertDoesNotExist()
            composeTestRule.onNodeWithText(compactLabel).assertDoesNotExist()
        }
    }

    @Test
    fun `button is enabled when not processing`() {
        val fullLabel = "Sign in with YouVersion"

        composeTestRule.setContent {
            SignInWithYouVersionButton(
                permissions = { emptySet() },
                mode = SignInWithYouVersionButtonMode.FULL,
                dark = true,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(fullLabel).assertIsEnabled()
        composeTestRule
            .onNode(hasProgressBarRangeInfo(androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
    }

    @Test
    fun `sign in error dialog dismisses on confirm`() {
        every { YouVersionAuthentication.signIn(any(), any(), any()) } throws RuntimeException("sign in failed")

        composeTestRule.setContent {
            SignInWithYouVersionButton(
                permissions = { emptySet() },
                mode = SignInWithYouVersionButtonMode.FULL,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sign in with YouVersion").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sign-In Failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sign-In Failed").assertDoesNotExist()
    }

    @Test
    fun `sign in error dialog dismisses on back press`() {
        every { YouVersionAuthentication.signIn(any(), any(), any()) } throws RuntimeException("sign in failed")

        composeTestRule.setContent {
            SignInWithYouVersionButton(
                permissions = { emptySet() },
                mode = SignInWithYouVersionButtonMode.FULL,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sign in with YouVersion").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sign-In Failed").assertIsDisplayed()

        composeTestRule.runOnUiThread {
            val dialog =
                assertNotNull(
                    ShadowDialog.getLatestDialog() as? ComponentDialog,
                    "Expected latest dialog to be ComponentDialog so back press can dismiss SignInErrorAlert",
                )
            dialog.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sign-In Failed").assertDoesNotExist()
    }
}
