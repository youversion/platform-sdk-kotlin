package com.youversion.platform

import android.app.Application
import com.youversion.platform.core.YouVersionPlatformConfiguration

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        YouVersionPlatformConfiguration.configure(
            context = this,
            appKey = TODO("Provide your app key"),
            appName = "Sample App",
            signInPromptMessage = getString(R.string.sign_in_prompt),
        )
    }
}
