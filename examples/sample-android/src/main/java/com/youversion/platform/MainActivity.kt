package com.youversion.platform

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.youversion.platform.ui.App
import com.youversion.platform.ui.signin.SignInWithYouVersionActivity
import com.youversion.platform.ui.theme.YouVersionPlatformTheme

class MainActivity : SignInWithYouVersionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YouVersionPlatformTheme {
                App()
            }
        }
    }
}
