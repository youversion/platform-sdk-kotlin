package com.youversion.platform

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youversion.platform.ui.App
import com.youversion.platform.ui.signin.SignInViewModel
import com.youversion.platform.ui.signin.YouVersionAuthentication
import com.youversion.platform.ui.theme.YouVersionPlatformTheme
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        if (YouVersionAuthentication.isAuthFlowActive.get()) {
            YouVersionAuthentication.cancelAuthentication()
        }
    }

    fun handleOAuthCallback(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            YouVersionAuthentication.handleAuthCallback(intent)
            clearIntent()
        }
    }

    private fun clearIntent() {
        intent = Intent(this, MainActivity::class.java)
    }
}
