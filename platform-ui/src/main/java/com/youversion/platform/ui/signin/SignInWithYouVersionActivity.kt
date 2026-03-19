package com.youversion.platform.ui.signin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import kotlin.getValue

abstract class SignInWithYouVersionActivity : ComponentActivity() {
    private val signInViewModel by viewModels<SignInViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (YouVersionAuthentication.isAuthenticationInProgress(this)) {
            YouVersionAuthentication.cancelAuthentication(this)
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.data != null) {
            signInViewModel.onAction(SignInViewModel.Action.ProcessAuthCallback(intent))
        }
    }
}
