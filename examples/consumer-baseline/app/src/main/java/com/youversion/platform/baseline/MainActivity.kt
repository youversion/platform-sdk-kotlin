package com.youversion.platform.baseline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.reader.BibleReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        YouVersionPlatformConfiguration.configure(
            context = applicationContext,
            appKey = "YOUR_APP_KEY_HERE",
        )
        setContent {
            BibleReader()
        }
    }
}
