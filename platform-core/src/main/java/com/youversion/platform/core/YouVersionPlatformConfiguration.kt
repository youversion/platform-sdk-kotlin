package com.youversion.platform.core

import android.content.Context
import androidx.annotation.RestrictTo
import co.touchlab.kermit.Logger
import com.youversion.platform.core.YouVersionPlatformConfiguration.configure
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.utilities.exceptions.YouVersionNotConfiguredException
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import com.youversion.platform.core.utilities.koin.startCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

object YouVersionPlatformConfiguration {
    private const val DEFAULT_API_HOST = "api.youversion.com"
    private const val DEFAULT_AUTH_CALLBACK = "youversionauth://callback"
    private val _configState = MutableStateFlow<Config?>(null)
    val configState = _configState.asStateFlow()

    private val config: Config?
        get() = _configState.value
    val appKey: String?
        get() = config?.appKey

    /**
     * The name of the host app, shown in the sign-in prompt so the reader knows who is asking. When `null` or
     * blank, the prompt names the host app by its launcher label.
     */
    val appName: String?
        get() = config?.appName

    /**
     * The host app's own reason for asking the reader to sign in, shown above the standard explanation in the
     * sign-in prompt. Omitted from the prompt when `null` or blank. Supports `**bold**` markdown.
     */
    val signInPromptMessage: String?
        get() = config?.signInPromptMessage

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

    /**
     * When set, only Bible versions whose `languageTag` is in this set are made available
     * in the version picker UI and other version listings. When `null` (the default), versions
     * in all languages are available. Tags follow BCP 47 (e.g. `"en"` for English).
     */
    val permittedLanguageTags: Set<String>?
        get() = config?.permittedLanguageTags

    /**
     * When set, only Bible versions whose `id` is in this set are made available in the
     * version picker UI and other version listings. When `null` (the default), all versions
     * are available. Combines with [permittedLanguageTags] — a version must satisfy both
     * filters to be available.
     */
    val permittedVersionIds: Set<Int>?
        get() = config?.permittedVersionIds

    /**
     * Whether the SDK may offer to sign the user in to YouVersion.
     *
     * When `false`, the reader hides the highlight colors from a signed-out reader rather than offering a control
     * that could never work, and a highlight action cannot start a sign-in flow. A reader who is already signed in
     * keeps their highlight colors, since they need nothing further. Defaults to `true`.
     */
    val isSignInEnabled: Boolean
        get() = config?.isSignInEnabled ?: true

    /**
     * The permissions the signed-in user has granted to this app. Empty when signed out, or when
     * the user has granted nothing.
     */
    val grantedPermissions: Set<SignInWithYouVersionPermission>
        get() = config?.grantedPermissions.orEmpty()

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
        permittedLanguageTags: Set<String>? = null,
        permittedVersionIds: Set<Int>? = null,
        isSignInEnabled: Boolean = true,
        appName: String? = null,
        signInPromptMessage: String? = null,
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
            permittedLanguageTags = permittedLanguageTags,
            permittedVersionIds = permittedVersionIds,
            isSignInEnabled = isSignInEnabled,
            appName = appName,
            signInPromptMessage = signInPromptMessage,
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
        permittedLanguageTags: Set<String>? = null,
        permittedVersionIds: Set<Int>? = null,
        isSignInEnabled: Boolean = true,
        appName: String? = null,
        signInPromptMessage: String? = null,
    ) {
        val sessionRepository = PlatformCoreKoinComponent.sessionRepository
        val previousConfig = config

        // The tokens a caller passes describe one session. Filling in the ones they left out from storage is safe
        // only when storage holds that same session, which is the case when they passed none at all, passed the
        // stored access token, or named the stored session. Otherwise the SDK would report one user while its
        // requests carried another's token, and that user would inherit the other's granted permissions.
        val keepsStoredSession =
            (accessToken == null && refreshToken == null && idToken == null) ||
                accessToken == sessionRepository.accessToken ||
                YouVersionApi.sessionId(accessToken, refreshToken, idToken) ==
                YouVersionApi.sessionId(
                    accessToken = sessionRepository.accessToken,
                    refreshToken = sessionRepository.refreshToken,
                    idToken = sessionRepository.idToken,
                )

        // Cleared rather than left in storage for a later saveAuthData to restore, since that compares the tokens it
        // is given against the configuration rather than against storage.
        if (!keepsStoredSession) {
            sessionRepository.grantedPermissionValues = emptySet()
        }

        _configState.value =
            Config(
                appKey = appKey,
                authCallback = authCallback,
                apiHost = apiHost,
                hostEnv = hostEnv,
                installId = sessionRepository.installId,
                accessToken = accessToken ?: sessionRepository.accessToken.takeIf { keepsStoredSession },
                refreshToken = refreshToken ?: sessionRepository.refreshToken.takeIf { keepsStoredSession },
                idToken = idToken ?: sessionRepository.idToken.takeIf { keepsStoredSession },
                expiryDate = expiryDate ?: sessionRepository.expiryDate.takeIf { keepsStoredSession },
                permittedLanguageTags = permittedLanguageTags,
                permittedVersionIds = permittedVersionIds,
                grantedPermissions = sessionRepository.grantedPermissionValues.toGrantedPermissions(),
                isSignInEnabled = isSignInEnabled,
                appName = appName,
                signInPromptMessage = signInPromptMessage,
            )

        // The Koin graph (and therefore the BibleVersionRepository singleton) survives reconfiguration
        // through the internal overload, so the in-memory version listings — which were filtered using
        // the previous configuration — must be invalidated when the filters change.
        val filtersChanged =
            previousConfig != null &&
                (
                    previousConfig.permittedLanguageTags != permittedLanguageTags ||
                        previousConfig.permittedVersionIds != permittedVersionIds
                )
        if (filtersChanged) {
            PlatformCoreKoinComponent.bibleVersionRepository.clearVersionListings()
        }
    }

    /**
     * Sets the copy the sign-in prompt shows, leaving the rest of the configuration alone.
     *
     * Meant only for the deprecated reader parameters that used to carry these values, so that a host app passing
     * them still gets the prompt it expects. New code should pass [appName] and [signInPromptMessage] to [configure].
     *
     * @param appName The name of the host app, shown in the sign-in prompt.
     * @param signInPromptMessage The host app's own reason for asking, replacing any already configured.
     * @throws YouVersionNotConfiguredException If [configure] has not been called first.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun configureSignIn(
        appName: String,
        signInPromptMessage: String?,
    ) {
        val currentConfig = config ?: throw YouVersionNotConfiguredException()
        _configState.value = currentConfig.copy(appName = appName, signInPromptMessage = signInPromptMessage)
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
     *
     * Granted permissions are kept when the tokens belong to the session already signed in, so that refreshing a
     * token does not revoke them. Tokens identifying a different session — a second user signing in without an
     * intervening [clearAuthData] — drop them instead, since one user's consent cannot stand in for another's.
     */
    fun saveAuthData(
        accessToken: String?,
        refreshToken: String?,
        idToken: String?,
        expiryDate: Date?,
        persist: Boolean = true,
    ) {
        val currentConfig = config ?: throw YouVersionNotConfiguredException()
        val isSameSession =
            YouVersionApi.sessionId(
                accessToken = currentConfig.accessToken,
                refreshToken = currentConfig.refreshToken,
                idToken = currentConfig.idToken,
            ) == YouVersionApi.sessionId(accessToken, refreshToken, idToken)

        writeAuthData(
            accessToken = accessToken,
            refreshToken = refreshToken,
            idToken = idToken,
            expiryDate = expiryDate,
            grantedPermissionValues =
                if (isSameSession) PlatformCoreKoinComponent.sessionRepository.grantedPermissionValues else emptySet(),
            persist = persist,
        )
    }

    /**
     * Writes auth data and granted permissions in a single configuration emission, so that
     * observers never see the two disagree.
     *
     * Permissions are carried as raw values rather than parsed ones so that a stored value this
     * SDK version does not recognize is preserved rather than dropped on every write.
     */
    private fun writeAuthData(
        accessToken: String?,
        refreshToken: String?,
        idToken: String?,
        expiryDate: Date?,
        grantedPermissionValues: Set<String>,
        persist: Boolean,
    ) {
        val currentConfig = config ?: throw YouVersionNotConfiguredException()

        _configState.value =
            currentConfig.copy(
                accessToken = accessToken,
                refreshToken = refreshToken,
                idToken = idToken,
                expiryDate = expiryDate,
                grantedPermissions = grantedPermissionValues.toGrantedPermissions(),
            )

        if (persist) {
            val sessionRepository = PlatformCoreKoinComponent.sessionRepository
            sessionRepository.accessToken = accessToken
            sessionRepository.refreshToken = refreshToken
            sessionRepository.idToken = idToken
            sessionRepository.expiryDate = expiryDate
            sessionRepository.grantedPermissionValues = grantedPermissionValues
        }
    }

    /**
     * Clears all persisted user authentication data from the device.
     *
     * This function effectively signs the user out of the application. It removes the
     * access token, refresh token, expiry date, and granted permissions from both the
     * in-memory cache and the secure, persistent storage.
     *
     * Call this function when the user explicitly chooses to sign out. After this is
     * called, the user will need to go through the `signIn` flow again to
     * re-authenticate.
     *
     * Any permission-granting flow still in progress is forgotten along with the tokens, so signing out leaves no
     * trace of who was signed in. A grant whose callback arrives afterwards is dropped by [completePermissionGrant]
     * rather than landing on the next user.
     */
    fun clearAuthData() {
        writeAuthData(
            accessToken = null,
            refreshToken = null,
            idToken = null,
            expiryDate = null,
            grantedPermissionValues = emptySet(),
            persist = true,
        )
        PlatformCoreKoinComponent.sessionRepository.permissionGrantSessionId = null
    }

    /**
     * Records permissions the user has granted, merging them with any already held.
     *
     * Granted permissions are persisted separately from the auth tokens so that refreshing a token
     * does not revoke them, and are cleared by [clearAuthData] when the user signs out or by
     * [saveAuthData] when the tokens it is given identify a different user.
     *
     * @param permissions The permissions the user granted.
     * @throws YouVersionNotConfiguredException If [configure] has not been called first.
     */
    fun saveGrantedPermissions(permissions: Collection<SignInWithYouVersionPermission>) {
        val currentConfig = config ?: throw YouVersionNotConfiguredException()
        val sessionRepository = PlatformCoreKoinComponent.sessionRepository

        val mergedValues = sessionRepository.grantedPermissionValues + permissions.map { it.rawValue }
        sessionRepository.grantedPermissionValues = mergedValues

        _configState.value = currentConfig.copy(grantedPermissions = mergedValues.toGrantedPermissions())
    }

    /**
     * Records that a permission-granting flow is starting for the session signed in now, so the grant it produces can
     * be checked against the session that earned it. Call this immediately before sending the user off to grant.
     *
     * Paired with [completePermissionGrant] and meant only for the SDK's own permission flows, which live in
     * platform-ui and so cannot reach an `internal` declaration here.
     *
     * @throws YouVersionNotConfiguredException If [configure] has not been called first.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun beginPermissionGrant() {
        config ?: throw YouVersionNotConfiguredException()
        PlatformCoreKoinComponent.sessionRepository.permissionGrantSessionId = YouVersionApi.currentSessionId
    }

    /**
     * Persists [permissions] only when the session that called [beginPermissionGrant] is still the one signed in, so
     * that a user cannot inherit consent another user gave.
     *
     * The comparison survives process death, since [beginPermissionGrant] records to storage — the granting flow
     * leaves the app for a browser, and the callback can arrive in a freshly created process. A grant with no
     * recorded flow behind it, or one arriving with nobody signed in, is dropped rather than attributed to whoever
     * happens to be signed in: the record is only absent when it was never written or a sign-out cleared it, and
     * neither says the granting user is still here.
     *
     * Paired with [beginPermissionGrant] and meant only for the SDK's own permission flows, which live in
     * platform-ui and so cannot reach an `internal` declaration here.
     *
     * @param permissions The permissions the user granted.
     * @return Whether they were persisted.
     * @throws YouVersionNotConfiguredException If [configure] has not been called first.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun completePermissionGrant(permissions: Collection<SignInWithYouVersionPermission>): Boolean {
        config ?: throw YouVersionNotConfiguredException()
        val startingSessionId = PlatformCoreKoinComponent.sessionRepository.permissionGrantSessionId
        if (startingSessionId == null || startingSessionId != YouVersionApi.currentSessionId) return false
        saveGrantedPermissions(permissions)
        return true
    }

    private fun Set<String>.toGrantedPermissions(): Set<SignInWithYouVersionPermission> =
        mapNotNull { SignInWithYouVersionPermission.fromRawValue(it) }.toSet()

    /**
     * Resets the configuration state to its initial uninitialized state.
     *
     * This is intended for use in tests only.
     */
    internal fun reset() {
        _configState.value = null
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
    val permittedLanguageTags: Set<String>? = null,
    val permittedVersionIds: Set<Int>? = null,
    val grantedPermissions: Set<SignInWithYouVersionPermission> = emptySet(),
    val isSignInEnabled: Boolean = true,
    val appName: String? = null,
    val signInPromptMessage: String? = null,
) {
    val isSignedIn = accessToken != null
}
