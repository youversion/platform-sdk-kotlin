package com.youversion.platform.ui.signin

import android.util.Log
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.users.model.SignInWithYouVersionResult

/**
 * A reusable Composable effect that provides a single suspend function to perform
 * the full YouVersion sign-in flow, similar to the Swift SDK's
 * `YouVersionAPI.Users.signIn(permissions:contextProvider:)`.
 *
 * This is a thin Compose wrapper around [SignInHandler], which can also be used
 * directly from non-Compose contexts such as React Native modules.
 *
 * @return A suspend function that accepts a set of [SignInWithYouVersionPermission]
 *         and returns a [SignInWithYouVersionResult] on success, or null on failure.
 */
@Composable
fun rememberSignIn(): suspend (Set<SignInWithYouVersionPermission>) -> SignInWithYouVersionResult? {
    val context = LocalContext.current
    val registryOwner = LocalActivityResultRegistryOwner.current
    if (registryOwner == null) {
        Log.w("YouVersionSignIn", "rememberSignIn() called without an ActivityResultRegistryOwner")
        return remember { { _ -> null } }
    }
    val handler = remember { SignInHandler(context, registryOwner.activityResultRegistry) }
    return remember { { permissions -> handler.signIn(permissions) } }
}
