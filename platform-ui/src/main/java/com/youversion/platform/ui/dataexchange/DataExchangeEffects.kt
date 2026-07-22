package com.youversion.platform.ui.dataexchange

import android.util.Log
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission

/**
 * A reusable Composable effect that provides a single suspend function to run the YouVersion data exchange
 * permission flow. A thin Compose wrapper around [DataExchangeHandler], mirroring
 * [com.youversion.platform.ui.signin.rememberSignIn].
 *
 * @return A suspend function that accepts the permissions to ask for and returns the flow's [DataExchangeResult],
 *         or null when there is no [androidx.activity.result.ActivityResultRegistryOwner] to launch from.
 */
@Composable
fun rememberDataExchange(): suspend (Set<SignInWithYouVersionPermission>) -> DataExchangeResult? {
    val registryOwner = LocalActivityResultRegistryOwner.current
    if (registryOwner == null) {
        Log.w("YouVersionDataExchange", "rememberDataExchange() called without an ActivityResultRegistryOwner")
        return remember { { _ -> null } }
    }
    val handler = remember { DataExchangeHandler(registryOwner.activityResultRegistry) }
    return remember { { permissions -> handler.requestDataExchange(permissions) } }
}
