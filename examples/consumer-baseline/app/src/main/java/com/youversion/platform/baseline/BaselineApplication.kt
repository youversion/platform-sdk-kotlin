package com.youversion.platform.baseline

import android.app.Application
import com.youversion.platform.core.YouVersionPlatformConfiguration

class BaselineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        YouVersionPlatformConfiguration.configure(
            context = this,
            appKey = "YOUR_APP_KEY_HERE",
        )
    }
}
