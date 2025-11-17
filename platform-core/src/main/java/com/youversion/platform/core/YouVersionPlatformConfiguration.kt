package com.youversion.platform.core

import android.content.Context
import com.youversion.platform.core.YouVersionPlatformConfiguration.configure
import com.youversion.platform.core.utilities.koin.YouVersionPlatformComponent
import com.youversion.platform.core.utilities.koin.startYouVersionPlatform
import java.util.UUID

object YouVersionPlatformConfiguration {
    private const val DEFAULT_API_HOST = "api.youversion.com"

    private var config: Config? = null

    val appKey: String?
        get() = config?.appKey
    val apiHost: String
        get() = config?.apiHost ?: DEFAULT_API_HOST
    val hostEnv: String?
        get() = config?.hostEnv
    val installId: String?
        get() = config?.installId
    val accessToken: String?
        get() = config?.accessToken

    fun configure(
        context: Context,
        appKey: String?,
        accessToken: String? = null,
        apiHost: String = DEFAULT_API_HOST,
        hostEnv: String? = null,
    ) {
        startYouVersionPlatform(context)

        // Now configure the SDK with whatever [Store] comes back from Koin
        configure(
            appKey = appKey,
            accessToken = accessToken,
            apiHost = apiHost,
            hostEnv = hostEnv,
        )
    }

    internal fun configure(
        appKey: String?,
        accessToken: String? = null,
        apiHost: String = DEFAULT_API_HOST,
        hostEnv: String? = null,
    ) {
        val store = YouVersionPlatformComponent.store

        config =
            Config(
                appKey = appKey,
                apiHost = apiHost,
                hostEnv = hostEnv,
                installId = store.installId ?: UUID.randomUUID().toString().also { store.installId = it },
                accessToken = accessToken ?: store.accessToken,
            )
    }

    /**
     * Updates the [accessToken] to be used by the SDK.
     *
     * @param accessToken The new accessToken to be used by the SDK
     * @param persist Stores the access token to local cache. Default true.
     * @throws IllegalStateException If [configure] has not been called first.
     */
    fun setAccessToken(
        accessToken: String?,
        persist: Boolean = true,
    ) {
        config?.let {
            config = it.copy(accessToken = accessToken)
            if (persist) {
                val store = YouVersionPlatformComponent.store
                store.accessToken = accessToken
            }
        } ?: throw IllegalStateException("You must first call configure")
    }
}

private data class Config(
    val appKey: String?,
    val apiHost: String,
    val hostEnv: String?,
    val installId: String?,
    val accessToken: String?,
)
