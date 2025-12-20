package com.youversion.platform.core

import android.content.Context
import co.touchlab.kermit.Logger
import com.youversion.platform.core.YouVersionPlatformConfiguration.configure
import com.youversion.platform.core.utilities.exceptions.YouVersionNotConfiguredException
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import com.youversion.platform.core.utilities.koin.startCore
import com.youversion.platform.foundation.PlatformKoinGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import java.util.UUID

object YouVersionPlatformConfiguration {
    private const val DEFAULT_API_HOST = "api.youversion.com"
    private const val DEFAULT_AUTH_CALLBACK = "youversionauth://callback"
    private val _configState = MutableStateFlow<Config?>(null)
    val configState = _configState.asStateFlow()

    private val config: Config?
        get() = _configState.value
    val appKey: String?
        get() = config?.appKey
    val authCallback: String
        get() = config?.authCallback ?: DEFAULT_AUTH_CALLBACK
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
    val idToken: String?
        get() = config?.idToken
    val expiryDate: Date?
        get() = config?.expiryDate

    val isSignedIn: Boolean
        get() = accessToken != null

    fun configure(
        context: Context,
        appKey: String?,
        authCallback: String = DEFAULT_AUTH_CALLBACK,
        accessToken: String? = null,
        refreshToken: String? = null,
        idToken: String? = null,
        expiryDate: Date? = null,
        apiHost: String = DEFAULT_API_HOST,
        hostEnv: String? = null,
    ) {
        if (config != null) {
            Logger.w("YouVersionPlatform SDK has already been configured. Reconfiguring.")
            _configState.value = null // Emit a null state to notify observers of reconfiguration
            PlatformKoinGraph.stop()
        }

        // This establishes the Dependency Graph which can only be called once.
        PlatformKoinGraph.startCore(context)

        // Now configure the SDK, use DI to provide any dependencies needed during configuration.
        configure(
            appKey = appKey,
            authCallback = authCallback,
            accessToken = accessToken,
            refreshToken = refreshToken,
            idToken = idToken,
            expiryDate = expiryDate,
            apiHost = apiHost,
            hostEnv = hostEnv,
        )
    }

    internal fun configure(
        appKey: String?,
        authCallback: String = DEFAULT_AUTH_CALLBACK,
        accessToken: String? = null,
        refreshToken: String? = null,
        idToken: String? = null,
        expiryDate: Date? = null,
        apiHost: String = DEFAULT_API_HOST,
        hostEnv: String? = null,
    ) {
        val store = PlatformCoreKoinComponent.store

        _configState.value =
            Config(
                appKey = appKey,
                authCallback = authCallback,
                apiHost = apiHost,
                hostEnv = hostEnv,
                installId = store.installId ?: UUID.randomUUID().toString().also { store.installId = it },
                accessToken = accessToken ?: store.accessToken,
                refreshToken = refreshToken ?: store.refreshToken,
                idToken = idToken ?: store.idToken,
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
     * @param idToken A JSON Web Token (JWT) that contains the user's identity and profile information.
     *                It proves that the user has been authenticated. Passing null will clear the
     *                stored ID token.
     * @param expiryDate The future date and time at which the access token becomes invalid.
     *                   Passing null will clear the stored expiry date.
     */
    fun saveAuthData(
        accessToken: String?,
        refreshToken: String?,
        idToken: String?,
        expiryDate: Date?,
        persist: Boolean = true,
    ) {
        val currentConfig = config ?: throw YouVersionNotConfiguredException()

        _configState.value =
            currentConfig.copy(
                accessToken = accessToken,
                refreshToken = refreshToken,
                idToken = idToken,
                expiryDate = expiryDate,
            )

        if (persist) {
            val store = PlatformCoreKoinComponent.store
            store.accessToken = accessToken
            store.refreshToken = refreshToken
            store.idToken = idToken
            store.expiryDate = expiryDate
        }
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
        saveAuthData(accessToken = null, refreshToken = null, idToken = null, expiryDate = null)
    }

    /**
     * Updates the [apiHost] to be used by the SDK.
     *
     * @param apiHost The new apiHost to be used by the SDK
     * @throws YouVersionNotConfiguredException If [configure] has not been called first.
     */
    fun setApiHost(apiHost: String) {
        val currentConfig = config ?: throw YouVersionNotConfiguredException()
        _configState.value = currentConfig.copy(apiHost = apiHost)
    }
}

data class Config(
    val appKey: String?,
    val authCallback: String,
    val apiHost: String,
    val hostEnv: String?,
    val installId: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val idToken: String?,
    val expiryDate: Date?,
) {
    val isSignedIn = accessToken != null
}
