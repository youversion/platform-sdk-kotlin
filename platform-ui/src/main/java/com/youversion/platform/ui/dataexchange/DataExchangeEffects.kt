package com.youversion.platform.ui.dataexchange

import android.util.Log
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission

/**
 * A reusable Composable effect that provides a single suspend function to run the YouVersion data exchange
 * permission flow. A thin Compose wrapper around [DataExchangeHandler], mirroring
 * [com.youversion.platform.ui.signin.rememberSignInWithPermissionValues].
 *
 * @param onBrowserOpened Called once the browser is open, and never called when it could not be opened. See
 *        [DataExchangeHandler.requestDataExchange].
 * @return A suspend function that accepts the raw values of the permissions to ask for, such as
 *         [com.youversion.platform.core.users.model.YouVersionPermissions.HIGHLIGHTS], and returns the flow's
 *         [DataExchangeResult], or null when there is no [androidx.activity.result.ActivityResultRegistryOwner] to
 *         launch from.
 */
@Composable
public fun rememberDataExchangeWithPermissionValues(
    onBrowserOpened: () -> Unit = {},
): suspend (Set<String>) -> DataExchangeResult? {
    val registryOwner = LocalActivityResultRegistryOwner.current
    if (registryOwner == null) {
        Log.w(
            "YouVersionDataExchange",
            "rememberDataExchangeWithPermissionValues() called without an ActivityResultRegistryOwner",
        )
        return remember { { _ -> null } }
    }
    val currentOnBrowserOpened by rememberUpdatedState(onBrowserOpened)
    val handler = remember { DataExchangeHandler(registryOwner.activityResultRegistry) }
    return remember {
        { permissions -> handler.requestDataExchange(permissions) { currentOnBrowserOpened() } }
    }
}

/**
 * A reusable Composable effect that provides a single suspend function to run the YouVersion data exchange
 * permission flow.
 *
 * @param onBrowserOpened Called once the browser is open, and never called when it could not be opened.
 * @return A suspend function that accepts the permissions to ask for and returns the flow's [DataExchangeResult],
 *         or null when there is no [androidx.activity.result.ActivityResultRegistryOwner] to launch from.
 */
@Deprecated(
    "Use rememberDataExchangeWithPermissionValues with raw String permission values instead.",
    ReplaceWith("rememberDataExchangeWithPermissionValues(onBrowserOpened)"),
)
@Composable
public fun rememberDataExchange(
    onBrowserOpened: () -> Unit = {},
): suspend (Set<SignInWithYouVersionPermission>) -> DataExchangeResult? {
    val requestDataExchange = rememberDataExchangeWithPermissionValues(onBrowserOpened)
    return remember(requestDataExchange) {
        { permissions -> requestDataExchange(permissions.map { it.rawValue }.toSet()) }
    }
}
