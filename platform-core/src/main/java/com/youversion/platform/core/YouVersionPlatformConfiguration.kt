package com.youversion.platform.core

import android.content.Context
import co.touchlab.kermit.Logger
import com.youversion.platform.core.utilities.exceptions.YouVersionNotConfiguredException
import com.youversion.platform.core.utilities.koin.YouVersionPlatformComponent
import com.youversion.platform.core.utilities.koin.startYouVersionPlatform
import com.youversion.platform.core.utilities.koin.stopYouVersionPlatform
import java.util.Date
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
    val refreshToken: String?
        get() = config?.refreshToken
    val expiryDate: Date?
        get() = config?.expiryDate

    fun configure(
        context: Context,
        appKey: String?,
        accessToken: String? = null,
        refreshToken: String? = null,
        expiryDate: Date? = null,
        apiHost: String = DEFAULT_API_HOST,
        hostEnv: String? = null,
    ) {
        if (config != null) {
            Logger.w("YouVersionPlatform SDK has already been configured. Reconfiguring.")
            config = null
            stopYouVersionPlatform()
        }

        // This establishes the Dependency Graph which can only be called once.
        startYouVersionPlatform(context)

        // Now configure the SDK, use DI to provide any dependencies needed during configuration.
        configure(
            appKey = appKey,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiryDate = expiryDate,
            apiHost = apiHost,
            hostEnv = hostEnv,
        )
    }

    internal fun configure(
        appKey: String?,
        accessToken: String? = null,
        refreshToken: String? = null,
        expiryDate: Date? = null,
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
                refreshToken = refreshToken ?: store.refreshToken,
                expiryDate = expiryDate ?: store.expiryDate,
            )
    }

    /**
     * Persists the authentication data received from a successful sign-in flow.
     *
     * This function should be called after a user has successfully authenticated and
     * tokens have been obtained from the token endpoint. It stores the access token,
     * refresh token, and token expiry date in a secure, persistent storage
     * (e.g., EncryptedSharedPreferences) so that the user remains signed in
     * across app sessions. It also caches these values in memory for immediate use.
     *
     * @param accessToken The OAuth 2.0 access token used for authorizing API requests.
     *                    Passing null will clear the stored access token.
     * @param refreshToken The token used to obtain a new access token when the current one
     *                     expires. Passing null will clear the stored refresh token.
     * @param expiryDate The future date and time at which the access token becomes invalid.
     *                   Passing null will clear the stored expiry date.
     */
    fun saveAuthData(
        accessToken: String?,
        refreshToken: String?,
        expiryDate: Date?,
        persist: Boolean = true,
    ) {
        config?.let {
            config =
                it.copy(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiryDate = expiryDate,
                )
            if (persist) {
                val store = YouVersionPlatformComponent.store
                store.accessToken = accessToken
                store.refreshToken = refreshToken
                store.expiryDate = expiryDate
            }
        } ?: throw YouVersionNotConfiguredException()
    }

    /**
     * Clears all persisted user authentication data from the device.
     *
     * This function effectively signs the user out of the application. It removes the
     * access token, refresh token, and expiry date from both the in-memory cache and
     * the secure, persistent storage.
     *
     * Call this function when the user explicitly chooses to sign out. After this is
     * called, the user will need to go through the `signIn` flow again to
     * re-authenticate.
     */
    fun clearAuthData() {
        saveAuthData(accessToken = null, refreshToken = null, expiryDate = null)
    }

    /**
     * Updates the [apiHost] to be used by the SDK.
     *
     * @param apiHost The new apiHost to be used by the SDK
     * @throws YouVersionNotConfiguredException If [configure] has not been called first.
     */
    fun setApiHost(apiHost: String) {
        config?.let {
            config = it.copy(apiHost = apiHost)
        } ?: throw YouVersionNotConfiguredException()
    }
}

private data class Config(
    val appKey: String?,
    val apiHost: String,
    val hostEnv: String?,
    val installId: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val expiryDate: Date?,
)
