package com.youversion.platform.ui.signin

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.test.core.app.ApplicationProvider
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.users.api.UsersApi
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.users.model.SignInWithYouVersionResult
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class YouVersionAuthenticationTests {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var mockUsersApi: UsersApi

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        mockUsersApi = mockk(relaxed = true)

        mockkObject(YouVersionPlatformConfiguration)
        mockkObject(PKCEStateStore)
        mockkObject(YouVersionApi)

        every { YouVersionPlatformConfiguration.appKey } returns "test-app-key"
        every { YouVersionPlatformConfiguration.authCallback } returns "youversionauth://callback"
        every { YouVersionApi.users } returns mockUsersApi
    }

    @AfterTest
    fun teardown() {
        unmockkObject(YouVersionPlatformConfiguration)
        unmockkObject(PKCEStateStore)
        unmockkObject(YouVersionApi)
        Dispatchers.resetMain()
    }

    // ----- signIn

    @Test
    fun `test signIn saves PKCE parameters to store`() {
        val launcher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)

        YouVersionAuthentication.signIn(
            context = context,
            launcher = launcher,
            permissions = setOf(SignInWithYouVersionPermission.OPENID),
        )

        verify { PKCEStateStore.save(context, any(), any(), any()) }
    }

    @Test
    fun `test signIn throws when appKey is null`() {
        every { YouVersionPlatformConfiguration.appKey } returns null
        val launcher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)

        assertFailsWith<YouVersionNetworkException> {
            YouVersionAuthentication.signIn(
                context = context,
                launcher = launcher,
                permissions = setOf(SignInWithYouVersionPermission.OPENID),
            )
        }
    }

    @Test
    fun `test signIn launches auth tab`() {
        val launcher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)

        YouVersionAuthentication.signIn(
            context = context,
            launcher = launcher,
            permissions = setOf(SignInWithYouVersionPermission.OPENID),
        )

        verify { launcher.launch(any()) }
    }

    // ----- handleAuthCallback

    @Test
    fun `test handleAuthCallback returns null when intent is null`() =
        runTest {
            assertNull(YouVersionAuthentication.handleAuthCallback(context, null))
        }

    @Test
    fun `test handleAuthCallback returns null when intent data is null`() =
        runTest {
            val intent = mockk<Intent>()
            every { intent.data } returns null

            assertNull(YouVersionAuthentication.handleAuthCallback(context, intent))
        }

    @Test
    fun `test handleAuthCallback returns null when scheme does not match`() =
        runTest {
            val intent = mockk<Intent>()
            every { intent.data } returns Uri.parse("https://wrong.com/callback?code=abc&state=xyz")

            assertNull(YouVersionAuthentication.handleAuthCallback(context, intent))
        }

    @Test
    fun `test handleAuthCallback returns null when stored state is null`() =
        runTest {
            val intent = intentWithValidCallback()
            every { PKCEStateStore.getState(context) } returns null
            every { PKCEStateStore.getCodeVerifier(context) } returns "verifier"
            every { PKCEStateStore.getNonce(context) } returns "nonce"
            every { PKCEStateStore.clear(context) } just Runs

            assertNull(YouVersionAuthentication.handleAuthCallback(context, intent))
        }

    @Test
    fun `test handleAuthCallback returns null when stored code verifier is null`() =
        runTest {
            val intent = intentWithValidCallback()
            every { PKCEStateStore.getState(context) } returns "state"
            every { PKCEStateStore.getCodeVerifier(context) } returns null
            every { PKCEStateStore.getNonce(context) } returns "nonce"
            every { PKCEStateStore.clear(context) } just Runs

            assertNull(YouVersionAuthentication.handleAuthCallback(context, intent))
        }

    @Test
    fun `test handleAuthCallback returns null when stored nonce is null`() =
        runTest {
            val intent = intentWithValidCallback()
            every { PKCEStateStore.getState(context) } returns "state"
            every { PKCEStateStore.getCodeVerifier(context) } returns "verifier"
            every { PKCEStateStore.getNonce(context) } returns null
            every { PKCEStateStore.clear(context) } just Runs

            assertNull(YouVersionAuthentication.handleAuthCallback(context, intent))
        }

    @Test
    fun `test handleAuthCallback clears PKCE store after retrieving values`() =
        runTest {
            val intent = intentWithValidCallback()
            every { PKCEStateStore.getState(context) } returns null
            every { PKCEStateStore.getCodeVerifier(context) } returns null
            every { PKCEStateStore.getNonce(context) } returns null
            every { PKCEStateStore.clear(context) } just Runs

            YouVersionAuthentication.handleAuthCallback(context, intent)

            verify { PKCEStateStore.clear(context) }
        }

    @Test
    fun `test handleAuthCallback returns result on success`() =
        runTest {
            val intent = intentWithValidCallback()
            val expectedResult = testSignInResult()
            stubPKCEStoreWithValues()
            coEvery {
                mockUsersApi.getSignInResult(any(), any(), any(), any(), any())
            } returns expectedResult
            every { YouVersionPlatformConfiguration.saveAuthData(any(), any(), any(), any()) } just Runs

            val result = YouVersionAuthentication.handleAuthCallback(context, intent)

            assertEquals(expectedResult, result)
        }

    @Test
    fun `test handleAuthCallback saves auth data on success`() =
        runTest {
            val intent = intentWithValidCallback()
            val expectedResult = testSignInResult()
            stubPKCEStoreWithValues()
            coEvery {
                mockUsersApi.getSignInResult(any(), any(), any(), any(), any())
            } returns expectedResult
            every { YouVersionPlatformConfiguration.saveAuthData(any(), any(), any(), any()) } just Runs

            YouVersionAuthentication.handleAuthCallback(context, intent)

            verify {
                YouVersionPlatformConfiguration.saveAuthData(
                    accessToken = expectedResult.accessToken,
                    refreshToken = expectedResult.refreshToken,
                    idToken = expectedResult.idToken,
                    expiryDate = expectedResult.expiryDate,
                )
            }
        }

    // ----- cancelAuthentication

    @Test
    fun `test cancelAuthentication clears PKCE store`() {
        every { PKCEStateStore.clear(context) } just Runs

        YouVersionAuthentication.cancelAuthentication(context)

        verify { PKCEStateStore.clear(context) }
    }

    // ----- isAuthenticationInProgress

    @Test
    fun `test isAuthenticationInProgress returns true when all PKCE values are stored`() {
        every { PKCEStateStore.getCodeVerifier(context) } returns "verifier"
        every { PKCEStateStore.getState(context) } returns "state"
        every { PKCEStateStore.getNonce(context) } returns "nonce"

        assertTrue(YouVersionAuthentication.isAuthenticationInProgress(context))
    }

    @Test
    fun `test isAuthenticationInProgress returns false when code verifier is null`() {
        every { PKCEStateStore.getCodeVerifier(context) } returns null
        every { PKCEStateStore.getState(context) } returns "state"
        every { PKCEStateStore.getNonce(context) } returns "nonce"

        assertFalse(YouVersionAuthentication.isAuthenticationInProgress(context))
    }

    @Test
    fun `test isAuthenticationInProgress returns false when state is null`() {
        every { PKCEStateStore.getCodeVerifier(context) } returns "verifier"
        every { PKCEStateStore.getState(context) } returns null
        every { PKCEStateStore.getNonce(context) } returns "nonce"

        assertFalse(YouVersionAuthentication.isAuthenticationInProgress(context))
    }

    @Test
    fun `test isAuthenticationInProgress returns false when nonce is null`() {
        every { PKCEStateStore.getCodeVerifier(context) } returns "verifier"
        every { PKCEStateStore.getState(context) } returns "state"
        every { PKCEStateStore.getNonce(context) } returns null

        assertFalse(YouVersionAuthentication.isAuthenticationInProgress(context))
    }

    // ----- helpers

    private fun intentWithValidCallback(): Intent {
        val intent = mockk<Intent>()
        every { intent.data } returns Uri.parse("youversionauth://callback?code=abc&state=xyz")
        return intent
    }

    private fun stubPKCEStoreWithValues() {
        every { PKCEStateStore.getState(context) } returns "stored-state"
        every { PKCEStateStore.getCodeVerifier(context) } returns "stored-verifier"
        every { PKCEStateStore.getNonce(context) } returns "stored-nonce"
        every { PKCEStateStore.clear(context) } just Runs
    }

    private fun testSignInResult() =
        SignInWithYouVersionResult(
            accessToken = "access-token",
            expiryDate = Date(),
            refreshToken = "refresh-token",
            idToken = "id-token",
            permissions = listOf(SignInWithYouVersionPermission.OPENID),
            yvpUserId = "user-123",
            name = "Test User",
            profilePicture = null,
            email = "test@example.com",
        )
}
