package com.youversion.platform.ui.views

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelProvider
import com.youversion.platform.core.Config
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.users.api.UsersApi
import com.youversion.platform.ui.signin.SignInViewModel
import com.youversion.platform.ui.signin.YouVersionAuthentication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignInWithYouVersionButtonTests {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var configStateFlow: MutableStateFlow<Config?>
    private lateinit var mockUsersApi: UsersApi

    @Before
    fun setUp() {
        mockkObject(YouVersionPlatformConfiguration)
        mockkObject(YouVersionApi)
        mockkObject(YouVersionAuthentication)

        configStateFlow = MutableStateFlow(null)

        every { YouVersionPlatformConfiguration.configState } returns configStateFlow

        mockUsersApi = mockk(relaxed = true)
        every { YouVersionApi.users } returns mockUsersApi
        every { mockUsersApi.currentUserName } returns null
        every { mockUsersApi.currentUserEmail } returns null
    }

    @After
    fun tearDown() {
        unmockkObject(YouVersionPlatformConfiguration)
        unmockkObject(YouVersionApi)
        unmockkObject(YouVersionAuthentication)
    }

    @Test
    fun `renders in FULL COMPACT ICON_ONLY with stroked filled and dark light`() {
        val fullLabel = "Sign in with YouVersion"
        val compactLabel = "Sign in"
        val iconLabel = "Bible Logo"

        val modeState = mutableStateOf(SignInWithYouVersionButtonMode.FULL)
        val strokedState = mutableStateOf(false)
        val darkState = mutableStateOf(true)

        composeTestRule.setContent {
            SignInWithYouVersionButton(
                permissions = { emptySet() },
                mode = modeState.value,
                stroked = strokedState.value,
                dark = darkState.value,
            )
        }

        val modes =
            listOf(
                SignInWithYouVersionButtonMode.FULL,
                SignInWithYouVersionButtonMode.COMPACT,
                SignInWithYouVersionButtonMode.ICON_ONLY,
            )

        for (mode in modes) {
            for (stroked in listOf(false, true)) {
                for (dark in listOf(true, false)) {
                    modeState.value = mode
                    strokedState.value = stroked
                    darkState.value = dark
                    composeTestRule.waitForIdle()

                    // Icon always renders.
                    composeTestRule.onNodeWithContentDescription(iconLabel).assertIsDisplayed()

                    when (mode) {
                        SignInWithYouVersionButtonMode.FULL -> {
                            composeTestRule.onNodeWithText(fullLabel).assertIsDisplayed()
                            composeTestRule.onNodeWithText(compactLabel).assertDoesNotExist()
                        }

                        SignInWithYouVersionButtonMode.COMPACT -> {
                            composeTestRule.onNodeWithText(compactLabel).assertIsDisplayed()
                            composeTestRule.onNodeWithText(fullLabel).assertDoesNotExist()
                        }

                        SignInWithYouVersionButtonMode.ICON_ONLY -> {
                            composeTestRule.onNodeWithText(fullLabel).assertDoesNotExist()
                            composeTestRule.onNodeWithText(compactLabel).assertDoesNotExist()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `button is disabled and shows progress indicator while processing`() {
        val fullLabel = "Sign in with YouVersion"
        val gate = CompletableDeferred<Unit>()
        coEvery { YouVersionAuthentication.handleAuthCallback(any(), any()) } coAnswers {
            gate.await()
            null
        }

        composeTestRule.setContent {
            SignInWithYouVersionButton(
                permissions = { emptySet() },
                mode = SignInWithYouVersionButtonMode.FULL,
                stroked = false,
                dark = true,
            )
        }
        composeTestRule.waitForIdle()

        val viewModel = ViewModelProvider(composeTestRule.activity).get(SignInViewModel::class.java)
        viewModel.onAction(SignInViewModel.Action.ProcessAuthCallback(Intent()))

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(fullLabel).assertIsNotEnabled()
        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()

        gate.complete(Unit)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(fullLabel).assertIsEnabled()
        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
    }
}
