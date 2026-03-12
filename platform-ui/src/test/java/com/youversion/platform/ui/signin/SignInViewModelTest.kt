package com.youversion.platform.ui.signin

import android.app.Application
import android.content.Intent
import app.cash.turbine.test
import com.youversion.platform.core.Config
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.users.api.UsersApi
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var mockUsersApi: UsersApi
    private lateinit var configStateFlow: MutableStateFlow<Config?>
    private lateinit var viewModel: SignInViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = RuntimeEnvironment.getApplication()
        mockUsersApi = mockk(relaxed = true)
        configStateFlow = MutableStateFlow(null)

        mockkObject(YouVersionPlatformConfiguration)
        mockkObject(YouVersionApi)
        mockkObject(YouVersionAuthentication)

        every { YouVersionPlatformConfiguration.configState } returns configStateFlow
        every { YouVersionPlatformConfiguration.clearAuthData() } just Runs
        every { YouVersionApi.users } returns mockUsersApi
        every { mockUsersApi.currentUserName } returns null
        every { mockUsersApi.currentUserEmail } returns null

        viewModel = SignInViewModel(application)
    }

    @AfterTest
    fun teardown() {
        unmockkObject(YouVersionPlatformConfiguration)
        unmockkObject(YouVersionApi)
        unmockkObject(YouVersionAuthentication)
        Dispatchers.resetMain()
    }

    // ---- Initial state

    @Test
    fun `initial state has expected defaults`() =
        runTest {
            with(viewModel.state.value) {
                assertFalse(isProcessing)
                assertFalse(isSignedIn)
                assertEquals(null, userName)
                assertEquals(null, userEmail)
                assertFalse(showSignOutConfirmation)
            }
        }

    // ---- Config state updates

    @Test
    fun `config state update with signed-in config sets isSignedIn to true`() =
        runTest {
            configStateFlow.emit(signedInConfig())
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isSignedIn)
        }

    @Test
    fun `config state update with null config sets isSignedIn to false`() =
        runTest {
            configStateFlow.emit(signedInConfig())
            advanceUntilIdle()
            configStateFlow.emit(null)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isSignedIn)
        }

    @Test
    fun `config state update populates userName and userEmail from UsersApi`() =
        runTest {
            every { mockUsersApi.currentUserName } returns "Jane Doe"
            every { mockUsersApi.currentUserEmail } returns "jane@example.com"

            configStateFlow.emit(signedInConfig())
            advanceUntilIdle()

            assertEquals("Jane Doe", viewModel.state.value.userName)
            assertEquals("jane@example.com", viewModel.state.value.userEmail)
        }

    // ---- ProcessAuthCallback

    @Test
    fun `ProcessAuthCallback sets isProcessing to false on success`() =
        runTest {
            coEvery { YouVersionAuthentication.handleAuthCallback(any(), any()) } returns null

            viewModel.onAction(SignInViewModel.Action.ProcessAuthCallback(mockk(relaxed = true)))
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isProcessing)
        }

    @Test
    fun `ProcessAuthCallback with handleAuthCallback returning null does not emit error event`() =
        runTest {
            coEvery { YouVersionAuthentication.handleAuthCallback(any(), any()) } returns null

            viewModel.events.test {
                viewModel.onAction(SignInViewModel.Action.ProcessAuthCallback(mockk(relaxed = true)))
                advanceUntilIdle()
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ProcessAuthCallback emits AuthenticationError event on exception`() =
        runTest {
            coEvery { YouVersionAuthentication.handleAuthCallback(any(), any()) } throws Exception("auth failed")

            viewModel.events.test {
                viewModel.onAction(SignInViewModel.Action.ProcessAuthCallback(mockk(relaxed = true)))
                advanceUntilIdle()
                assertEquals(SignInViewModel.Event.AuthenticationError, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ProcessAuthCallback resets isProcessing to false after exception`() =
        runTest {
            coEvery { YouVersionAuthentication.handleAuthCallback(any(), any()) } throws Exception("auth failed")

            viewModel.events.test {
                viewModel.onAction(SignInViewModel.Action.ProcessAuthCallback(mockk(relaxed = true)))
                advanceUntilIdle()
                awaitItem() // consume AuthenticationError so channel send does not block
                cancelAndIgnoreRemainingEvents()
            }

            assertFalse(viewModel.state.value.isProcessing)
        }

    // ---- SignOut

    @Test
    fun `SignOut with requireConfirmation true shows confirmation dialog`() =
        runTest {
            viewModel.onAction(SignInViewModel.Action.SignOut(requireConfirmation = true))

            assertTrue(viewModel.state.value.showSignOutConfirmation)
        }

    @Test
    fun `SignOut with requireConfirmation true does not call signOut`() =
        runTest {
            viewModel.onAction(SignInViewModel.Action.SignOut(requireConfirmation = true))

            verify(exactly = 0) { mockUsersApi.signOut() }
        }

    @Test
    fun `SignOut with requireConfirmation false calls signOut`() =
        runTest {
            viewModel.onAction(SignInViewModel.Action.SignOut(requireConfirmation = false))

            verify(exactly = 1) { mockUsersApi.signOut() }
        }

    @Test
    fun `SignOut with requireConfirmation false hides confirmation dialog`() =
        runTest {
            viewModel.onAction(SignInViewModel.Action.SignOut(requireConfirmation = true))
            viewModel.onAction(SignInViewModel.Action.SignOut(requireConfirmation = false))

            assertFalse(viewModel.state.value.showSignOutConfirmation)
        }

    // ---- CancelSignOut

    @Test
    fun `CancelSignOut sets showSignOutConfirmation to false`() =
        runTest {
            viewModel.onAction(SignInViewModel.Action.SignOut(requireConfirmation = true))
            assertTrue(viewModel.state.value.showSignOutConfirmation)

            viewModel.onAction(SignInViewModel.Action.CancelSignOut)

            assertFalse(viewModel.state.value.showSignOutConfirmation)
        }

    @Test
    fun `CancelSignOut is idempotent when confirmation is not showing`() =
        runTest {
            viewModel.onAction(SignInViewModel.Action.CancelSignOut)

            assertFalse(viewModel.state.value.showSignOutConfirmation)
        }

    // ---- UpdateSignInState

    @Test
    fun `UpdateSignInState with valid token does not clear auth data`() =
        runTest {
            coEvery { YouVersionApi.hasValidToken() } returns true

            viewModel.onAction(SignInViewModel.Action.UpdateSignInState)
            advanceUntilIdle()

            verify(exactly = 0) { YouVersionPlatformConfiguration.clearAuthData() }
        }

    @Test
    fun `UpdateSignInState with invalid token clears auth data`() =
        runTest {
            coEvery { YouVersionApi.hasValidToken() } returns false

            viewModel.onAction(SignInViewModel.Action.UpdateSignInState)
            advanceUntilIdle()

            verify(exactly = 1) { YouVersionPlatformConfiguration.clearAuthData() }
        }

    // ---- Factory

    @Test
    fun `factory returns a non-null ViewModelProvider Factory`() {
        assertNotNull(SignInViewModel.factory())
    }

    // ---- Helpers

    private fun signedInConfig() =
        Config(
            appKey = "test-app-key",
            authCallback = "youversionauth://callback",
            apiHost = "api.youversion.com",
            hostEnv = null,
            installId = null,
            accessToken = "test-access-token",
            refreshToken = "test-refresh-token",
            idToken = null,
            expiryDate = null,
        )
}
