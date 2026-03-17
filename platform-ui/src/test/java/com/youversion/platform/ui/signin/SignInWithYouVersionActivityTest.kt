package com.youversion.platform.ui.signin

import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignInWithYouVersionActivityTest {
    class TestSignInWithYouVersionActivity : SignInWithYouVersionActivity()

    @Before
    fun setUp() {
        mockkConstructor(SignInViewModel::class)
        mockkObject(YouVersionAuthentication)
        every { anyConstructed<SignInViewModel>().onAction(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onCreate with intent data calls ProcessAuthCallback`() {
        val uri = Uri.parse("youversionauth://callback")
        val intent = Intent(Intent.ACTION_VIEW, uri)

        Robolectric
            .buildActivity(TestSignInWithYouVersionActivity::class.java, intent)
            .create()
            .get()

        verify(exactly = 1) {
            anyConstructed<SignInViewModel>().onAction(
                match { action ->
                    action is SignInViewModel.Action.ProcessAuthCallback &&
                        action.intent.data == uri
                },
            )
        }
    }

    @Test
    fun `onCreate with no intent data does not call onAction`() {
        val intent = Intent()

        Robolectric
            .buildActivity(TestSignInWithYouVersionActivity::class.java, intent)
            .create()
            .get()

        verify(exactly = 0) {
            anyConstructed<SignInViewModel>().onAction(any())
        }
    }

    @Test
    fun `onNewIntent with data calls ProcessAuthCallback`() {
        val activityController =
            Robolectric.buildActivity(TestSignInWithYouVersionActivity::class.java).create()
        val activity = activityController.get()

        val uri = Uri.parse("youversionauth://callback")
        val newIntent = Intent(Intent.ACTION_VIEW, uri)

        activity.onNewIntent(newIntent)

        verify(exactly = 1) {
            anyConstructed<SignInViewModel>().onAction(
                match { action ->
                    action is SignInViewModel.Action.ProcessAuthCallback &&
                        action.intent.data == uri
                },
            )
        }
    }

    @Test
    fun `onNewIntent with no data does not call onAction`() {
        val activityController =
            Robolectric.buildActivity(TestSignInWithYouVersionActivity::class.java).create()
        val activity = activityController.get()

        val newIntent = Intent()

        activity.onNewIntent(newIntent)

        verify(exactly = 0) {
            anyConstructed<SignInViewModel>().onAction(any())
        }
    }

    @Test
    fun `onResume when authentication is in progress cancels authentication`() {
        every { YouVersionAuthentication.isAuthenticationInProgress(any()) } returns true
        every { YouVersionAuthentication.cancelAuthentication(any()) } just runs

        val activity =
            Robolectric
                .buildActivity(TestSignInWithYouVersionActivity::class.java)
                .create()
                .resume()
                .get()

        verify(exactly = 1) {
            YouVersionAuthentication.isAuthenticationInProgress(activity)
        }
        verify(exactly = 1) {
            YouVersionAuthentication.cancelAuthentication(activity)
        }
    }

    @Test
    fun `onResume when authentication is not in progress does not cancel authentication`() {
        every { YouVersionAuthentication.isAuthenticationInProgress(any()) } returns false

        val activity =
            Robolectric
                .buildActivity(TestSignInWithYouVersionActivity::class.java)
                .create()
                .resume()
                .get()

        verify(exactly = 1) {
            YouVersionAuthentication.isAuthenticationInProgress(activity)
        }
        verify(exactly = 0) {
            YouVersionAuthentication.cancelAuthentication(any())
        }
    }
}
