package com.youversion.platform

import android.app.Application
import com.youversion.platform.core.YouVersionPlatformConfiguration

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        YouVersionPlatformConfiguration.configure(
            context = this,
            appKey = TODO("Provide your app key"),
            apiHost = "api-staging.youversion.com",
        )
    }
}
