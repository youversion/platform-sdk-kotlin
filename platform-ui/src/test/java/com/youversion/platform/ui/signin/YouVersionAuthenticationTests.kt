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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
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

    companion object {
        private const val STORED_STATE = "stored-state"
        private const val STORED_CODE_VERIFIER = "stored-verifier"
        private const val STORED_NONCE = "stored-nonce"
        private const val VALID_CALLBACK_URI =
            "youversionauth://callback?code=abc&state=$STORED_STATE"
    }

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
        unmockkAll()
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

        verify(exactly = 1) { PKCEStateStore.save(context, any(), any(), any()) }
    }

    @Test
    fun `test signIn throws when appKey is null`() {
        every { YouVersionPlatformConfiguration.appKey } returns null
        val launcher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)

        val exception =
            assertFailsWith<YouVersionNetworkException> {
                YouVersionAuthentication.signIn(
                    context = context,
                    launcher = launcher,
                    permissions = setOf(SignInWithYouVersionPermission.OPENID),
                )
            }
        assertEquals(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION, exception.reason)
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
            coVerify {
                mockUsersApi.getSignInResult(
                    callbackUri = VALID_CALLBACK_URI,
                    state = STORED_STATE,
                    codeVerifier = STORED_CODE_VERIFIER,
                    redirectUri = "youversionauth://callback",
                    nonce = STORED_NONCE,
                )
            }
            verify { PKCEStateStore.clear(context) }
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
            verify { PKCEStateStore.clear(context) }
        }

    @Test
    fun `test handleAuthCallback throws on token exchange failure`() =
        runTest {
            val intent = intentWithValidCallback()
            stubPKCEStoreWithValues()
            coEvery {
                mockUsersApi.getSignInResult(any(), any(), any(), any(), any())
            } throws YouVersionNetworkException(YouVersionNetworkException.Reason.CANNOT_DOWNLOAD)
            every { YouVersionPlatformConfiguration.saveAuthData(any(), any(), any(), any()) } just Runs

            val exception =
                assertFailsWith<YouVersionNetworkException> {
                    YouVersionAuthentication.handleAuthCallback(context, intent)
                }
            assertEquals(YouVersionNetworkException.Reason.CANNOT_DOWNLOAD, exception.reason)
            verify { PKCEStateStore.clear(context) }
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
        every { intent.data } returns Uri.parse(VALID_CALLBACK_URI)
        return intent
    }

    private fun stubPKCEStoreWithValues() {
        every { PKCEStateStore.getState(context) } returns STORED_STATE
        every { PKCEStateStore.getCodeVerifier(context) } returns STORED_CODE_VERIFIER
        every { PKCEStateStore.getNonce(context) } returns STORED_NONCE
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
