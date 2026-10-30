package com.youversion.platform.baseline

import android.os.Bundle
import androidx.activity.compose.setContent
import com.youversion.platform.reader.BibleReader
import com.youversion.platform.ui.signin.SignInWithYouVersionActivity

class MainActivity : SignInWithYouVersionActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BibleReader()
        }
    }
}
