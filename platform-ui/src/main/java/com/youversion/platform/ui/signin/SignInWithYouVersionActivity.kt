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

        handleOAuthCallback(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    override fun onResume() {
        super.onResume()
        if (YouVersionAuthentication.isAuthenticationInProgress(this)) {
            YouVersionAuthentication.cancelAuthentication(this)
        }
    }

    private fun handleOAuthCallback(intent: Intent?) {
        println("handleOAuthCallback: intent=$intent")
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            signInViewModel.onAction(SignInViewModel.Action.ProcessAuthCallback(intent))
            clearIntent()
        }
    }

    private fun clearIntent() {
        intent = Intent(this, SignInWithYouVersionActivity::class.java)
    }
}
