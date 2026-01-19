package com.youversion.platform

import android.app.Application
import com.youversion.platform.core.YouVersionPlatformConfiguration

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        YouVersionPlatformConfiguration.configure(
            context = this,
            appKey = "WYLFZYcnZYIdvBt29qAYTC1XJU1jJSuFIgkHM7e9cuWAFuTN",
        )
    }
}
