package com.youversion.platform.ui.signin

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.users.model.SignInWithYouVersionPKCEAuthorizationRequest
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.users.model.SignInWithYouVersionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles the user authentication flow for signing in with YouVersion.
 */
object YouVersionAuthentication {
    /**
     * Presents the YouVersion login flow to the user. This function is now a 'fire-and-forget'
     * operation. The result must be handled by `handleAuthCallback`.
     *
     * @param context The Android context required to launch the Custom Tab.
     * @param permissions The set of permissions to request from the user.
     */
    fun signIn(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
        permissions: Set<SignInWithYouVersionPermission>,
    ) {
        val appKey =
            YouVersionPlatformConfiguration.appKey
                ?: throw YouVersionNetworkException(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION)

        val authorizationRequest =
            SignInWithYouVersionPKCEAuthorizationRequest(
                appKey = appKey,
                permissions = permissions,
                redirectUri = YouVersionPlatformConfiguration.authCallback,
            )

        PKCEStateStore.save(
            context,
            codeVerifier = authorizationRequest.parameters.codeVerifier,
            state = authorizationRequest.parameters.state,
            nonce = authorizationRequest.parameters.nonce,
        )

        val authTabIntent = AuthTabIntent.Builder().build()
        authTabIntent.launch(
            launcher,
            authorizationRequest.url.toUri(),
            YouVersionPlatformConfiguration.authCallback,
        )
    }

    /**
     * Handles the callback from the Custom Tab, completes the authentication, and
     * returns the result.
     *
     * @param context The Android context.
     * @param intent The intent received from the Custom Tab redirect.
     * @return A [SignInWithYouVersionResult] on success, or null on failure/cancellation.
     * @throws Exception if the token exchange fails.
     */
    suspend fun handleAuthCallback(
        context: Context,
        intent: Intent?,
    ): SignInWithYouVersionResult? {
        val callbackUri = intent?.data ?: return null

        if (callbackUri.scheme != YouVersionPlatformConfiguration.authCallback.toUri().scheme) {
            return null
        }

        val storedState = PKCEStateStore.getState(context)
        val storedCodeVerifier = PKCEStateStore.getCodeVerifier(context)
        val storedNonce = PKCEStateStore.getNonce(context)

        PKCEStateStore.clear(context)

        if (storedState == null || storedCodeVerifier == null || storedNonce == null) {
            return null
        }

        return withContext(Dispatchers.IO) {
            val result =
                YouVersionApi.users.getSignInResult(
                    callbackUri = callbackUri.toString(),
                    state = storedState,
                    codeVerifier = storedCodeVerifier,
                    redirectUri = YouVersionPlatformConfiguration.authCallback,
                    nonce = storedNonce,
                )

            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                idToken = result.idToken,
                expiryDate = result.expiryDate,
            )
            result
        }
    }

    /**
     * Clears the stored PKCE state in case of user-initiated cancellation.
     *
     * @param context The Android context.
     */
    fun cancelAuthentication(context: Context) {
        PKCEStateStore.clear(context)
    }

    /**
     * Checks if an authentication flow is currently in progress by looking for
     * stored PKCE parameters.
     *
     * This is useful for determining if the app should expect a callback from a
     * Custom Tab, especially after the app process has been recreated.
     *
     * @param context The Android context.
     * @return `true` if a code verifier, state, and nonce are stored, `false` otherwise.
     */
    fun isAuthenticationInProgress(context: Context): Boolean =
        PKCEStateStore.getCodeVerifier(context) != null &&
            PKCEStateStore.getState(context) != null &&
            PKCEStateStore.getNonce(context) != null
}
