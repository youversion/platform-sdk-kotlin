package com.youversion.platform

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.ui.App
import com.youversion.platform.ui.signin.SignInViewModel
import com.youversion.platform.ui.theme.YouVersionPlatformTheme

class MainActivity : ComponentActivity() {
    private val signInViewModel by viewModels<SignInViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println(YouVersionPlatformConfiguration.accessToken)

        handleOAuthCallback(intent)

        enableEdgeToEdge()
        setContent {
            YouVersionPlatformTheme {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    override fun onResume() {
        super.onResume()
        if (signInViewModel.isAuthenticationInProgress(this)) {
            signInViewModel.cancelAuthentication(this)
        }
    }

    fun handleOAuthCallback(intent: Intent?) {
        println("handleOAuthCallback: intent=$intent")
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            signInViewModel.handleAuthCallback(this, intent)
            clearIntent()
        }
    }

    private fun clearIntent() {
        intent = Intent(this, MainActivity::class.java)
    }
}
