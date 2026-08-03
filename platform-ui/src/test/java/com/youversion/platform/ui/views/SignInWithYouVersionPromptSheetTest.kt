package com.youversion.platform.ui.views

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.youversion.platform.core.Config
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SignInWithYouVersionPromptSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        ApplicationProvider.getApplicationContext<Context>().applicationInfo.nonLocalizedLabel = HOST_APP_LABEL
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun stubConfigState(config: Config?): MutableStateFlow<Config?> {
        mockkObject(YouVersionPlatformConfiguration)
        val configStateFlow = MutableStateFlow(config)
        every { YouVersionPlatformConfiguration.configState } returns configStateFlow
        return configStateFlow
    }

    private fun config(
        appName: String? = "Test App",
        signInPromptMessage: String? = null,
    ) = Config(
        appKey = "test",
        authCallback = "",
        apiHost = "",
        hostEnv = null,
        installId = null,
        accessToken = null,
        refreshToken = null,
        idToken = null,
        expiryDate = null,
        appName = appName,
        signInPromptMessage = signInPromptMessage,
    )

    private fun renderSheet(
        appName: String? = "Test App",
        signInPromptMessage: String? = null,
        onSignIn: () -> Unit = {},
        onDismissRequest: () -> Unit = {},
    ) {
        stubConfigState(config(appName = appName, signInPromptMessage = signInPromptMessage))

        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                SignInWithYouVersionPromptSheet(
                    onSignIn = onSignIn,
                    onDismissRequest = onDismissRequest,
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
    fun `names the host app by its launcher label when no app name is configured`() {
        renderSheet(appName = null)

        composeTestRule
            .onNodeWithText("$HOST_APP_LABEL wants to connect", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `names the host app by its launcher label when the configured app name is blank`() {
        renderSheet(appName = "   ")

        composeTestRule
            .onNodeWithText("$HOST_APP_LABEL wants to connect", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `picks up copy configured after it is already showing`() {
        val configStateFlow = stubConfigState(null)

        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                SignInWithYouVersionPromptSheet(onSignIn = {}, onDismissRequest = {})
            }
        }

        configStateFlow.value = config(appName = "Test App", signInPromptMessage = "Keep your highlights")
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Test App wants to connect", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep your highlights").assertIsDisplayed()
    }

    @Test
    fun `shows the host app's own message when it has one`() {
        renderSheet(signInPromptMessage = "Save your highlights across devices")

        composeTestRule
            .onNodeWithText("Save your highlights across devices")
            .assertIsDisplayed()
    }

    @Test
    fun `still explains itself when the host app has no message of its own`() {
        renderSheet(signInPromptMessage = null)

        composeTestRule
            .onNodeWithText("Test App wants to connect", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Yes Please").assertIsDisplayed()
    }

    @Test
    fun `omits the host app's message when it is blank`() {
        renderSheet(signInPromptMessage = "   ")

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

    private companion object {
        const val HOST_APP_LABEL = "Sample App"
    }
}
