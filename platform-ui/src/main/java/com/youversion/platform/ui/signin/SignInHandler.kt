package com.youversion.platform.ui.signin

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.users.model.SignInWithYouVersionResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A framework-agnostic handler for the YouVersion sign-in flow.
 *
 * This provides a single suspend function to perform the full authentication flow,
 * similar to the Swift SDK's `YouVersionAPI.Users.signIn(permissions:contextProvider:)`.
 * It can be used from Compose (via [rememberSignIn]), React Native modules, or any
 * other Android context that has access to a [Context] and [ActivityResultRegistry].
 *
 * @param context The Android context needed to launch the Custom Tab and handle callbacks.
 * @param activityResultRegistry The registry used to register for activity results.
 */
class SignInHandler(
    private val context: Context,
    private val activityResultRegistry: ActivityResultRegistry,
) {
    /**
     * Launches the full YouVersion sign-in flow and suspends until it completes.
     *
     * This registers a temporary activity result handler, opens the Custom Tab for
     * authentication, waits for the redirect callback, and exchanges the authorization
     * code for tokens.
     *
     * @param permissions The set of permissions to request from the user.
     * @return A [SignInWithYouVersionResult] on success, or null on failure/cancellation.
     * @throws Exception if the token exchange or sign-in launch fails.
     */
    suspend fun signIn(permissions: Set<SignInWithYouVersionPermission>): SignInWithYouVersionResult? {
        var launcher: ActivityResultLauncher<Intent>? = null
        val callbackIntent =
            try {
                suspendCancellableCoroutine { continuation ->
                    launcher =
                        activityResultRegistry.register(
                            "youversion-sign-in",
                            ActivityResultContracts.StartActivityForResult(),
                        ) { result ->
                            continuation.resume(result.data)
                        }

                    YouVersionAuthentication.signIn(context, launcher, permissions)
                }
            } finally {
                launcher?.unregister()
            }

        return YouVersionAuthentication.handleAuthCallback(context, callbackIntent)
    }
}
