package com.youversion.platform

import android.app.Application
import com.youversion.platform.core.YouVersionPlatformConfiguration

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        YouVersionPlatformConfiguration.configure(
            context = this,
            appKey = "M77XmvwImB5XigJNy7AcqTrjSzDbpJTftSQvSY7M10ud9LhC",
        )
    }
}
