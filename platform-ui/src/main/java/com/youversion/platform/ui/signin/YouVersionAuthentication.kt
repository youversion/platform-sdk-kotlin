package com.youversion.platform.ui.signin

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.users.model.SignInWithYouVersion
import com.youversion.platform.core.users.model.SignInWithYouVersionPKCEAuthorizationRequestBuilder
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.users.model.SignInWithYouVersionResult
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

/**
 * Handles the user authentication flow for signing in with YouVersion.
 */
object YouVersionAuthentication {
    // Holds the coroutine continuation across the Custom Tabs activity flow.
    private var authContinuation: ((Result<Uri>) -> Unit)? = null
    val isAuthFlowActive = AtomicBoolean(false)

    /**
     * Presents the YouVersion login flow and returns the result upon completion.
     *
     * This function uses Chrome Custom Tabs to authenticate the user. It suspends until the
     * user completes or cancels the login flow.
     *
     * @param context The Android context (e.g., an Activity) required to launch the Custom Tab.
     * @param permissions The set of permissions to request from the user.
     * @return A [SignInWithYouVersionResult] on successful login.
     * @throws Exception if authentication fails or is cancelled.
     */
    suspend fun signIn(
        context: Context,
        permissions: Set<SignInWithYouVersionPermission>,
    ): SignInWithYouVersionResult {
        val appKey =
            YouVersionPlatformConfiguration.appKey
                ?: throw YouVersionNetworkException(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION)

        val authorizationRequest =
            SignInWithYouVersionPKCEAuthorizationRequestBuilder.make(
                appKey = appKey,
                permissions = permissions,
                redirectUri = SignInWithYouVersion.redirectURL,
            )

        isAuthFlowActive.set(true)

        // Launch the Custom Tab
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, authorizationRequest.url)

        // Suspend and wait for the callback from the deep link activity
        val callbackUri =
            suspendCancellableCoroutine { continuation ->
                authContinuation = { result ->
                    continuation.resumeWith(result)
                    isAuthFlowActive.set(false)
                    authContinuation = null
                }
                continuation.invokeOnCancellation {
                    authContinuation?.invoke(Result.failure(Exception("Sign in cancelled.")))
                    isAuthFlowActive.set(false)
                    authContinuation = null
                }
            }

        // Authentication flow continues here
        try {
            val location =
                SignInWithYouVersion.obtainLocation(
                    from = callbackUri,
                    state = authorizationRequest.parameters.state,
                )
            val code = SignInWithYouVersion.obtainCode(from = location)
            val tokens =
                SignInWithYouVersion.obtainTokens(
                    from = code,
                    codeVerifier = authorizationRequest.parameters.codeVerifier,
                )
            val result = SignInWithYouVersion.extractSignInWithYouVersionResult(from = tokens)

            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                expiryDate = result.expiryDate,
            )
            return result
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * This function should be called when the authentication flow is cancelled
     * by the user (e.g., closing the Custom Tab). It resumes the suspended
     * coroutine with a cancellation exception.
     */
    fun cancelAuthentication() {
        authContinuation?.invoke(
            Result.failure(
                CancellationException("Authentication flow was cancelled by the user."),
            ),
        )
        isAuthFlowActive.set(false)
        authContinuation = null // Clean up to prevent leaks
    }

    /**
     * This function must be called from your deep link handling Activity
     * to resume the suspended coroutine with the result.
     */
    fun handleAuthCallback(intent: Intent?) {
        isAuthFlowActive.set(false)
        val uri = intent?.data
        if (uri != null) {
            authContinuation?.invoke(Result.success(uri))
        } else {
            authContinuation?.invoke(Result.failure(Exception("Authentication cancelled or failed.")))
        }
        authContinuation = null
    }
}
