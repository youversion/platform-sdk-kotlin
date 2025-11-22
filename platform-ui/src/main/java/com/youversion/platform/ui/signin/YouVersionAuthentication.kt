package com.youversion.platform.ui.signin

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.users.model.SignInWithYouVersion
import com.youversion.platform.core.users.model.SignInWithYouVersionPKCEAuthorizationRequestBuilder
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
        permissions: Set<SignInWithYouVersionPermission>,
    ) {
        val appKey =
            YouVersionPlatformConfiguration.appKey
                ?: throw YouVersionNetworkException(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION)

        val authorizationRequest =
            SignInWithYouVersionPKCEAuthorizationRequestBuilder.make(
                appKey = appKey,
                permissions = permissions,
                redirectUri = SignInWithYouVersion.redirectURL,
            )

        PKCEStateStore.save(
            context,
            codeVerifier = authorizationRequest.parameters.codeVerifier,
            state = authorizationRequest.parameters.state,
        )

        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, authorizationRequest.url)
    }

    /**
     * Handles the callback from the Custom Tab, completes the authentication, and
     * returns the result. This function performs network operations and should be
     * called from a coroutine.
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
        val uri = intent?.data ?: return null

        val storedState = PKCEStateStore.getState(context)
        val storedCodeVerifier = PKCEStateStore.getCodeVerifier(context)

        PKCEStateStore.clear(context)

        if (storedState == null || storedCodeVerifier == null) {
            println("PKCE state not found. The sign-in flow may have been interrupted by process death.")
            return null
        }

        return withContext(Dispatchers.IO) {
            val location =
                SignInWithYouVersion.obtainLocation(
                    from = uri,
                    state = storedState,
                )
            val code = SignInWithYouVersion.obtainCode(from = location)
            val tokens =
                SignInWithYouVersion.obtainTokens(
                    from = code,
                    codeVerifier = storedCodeVerifier,
                )
            val result = SignInWithYouVersion.extractSignInWithYouVersionResult(from = tokens)

            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
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
     * @return `true` if a code verifier or state is stored, `false` otherwise.
     */
    fun isAuthenticationInProgress(context: Context): Boolean =
        PKCEStateStore.getCodeVerifier(context) != null &&
            PKCEStateStore.getState(context) != null
}
