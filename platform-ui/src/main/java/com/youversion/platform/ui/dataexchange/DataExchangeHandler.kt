package com.youversion.platform.ui.dataexchange

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.dataexchange.api.DataExchangeEndpoints
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A framework-agnostic handler for the YouVersion data exchange permission flow.
 *
 * Asking this way lets a signed-in user grant a permission without repeating the whole sign-in, so a permission can
 * be requested at the moment it is needed. Mirrors [com.youversion.platform.ui.signin.SignInHandler], and can be
 * used from Compose (via [rememberDataExchange]) or any other Android context with an [ActivityResultRegistry].
 *
 * @param activityResultRegistry The registry used to register for the callback.
 */
class DataExchangeHandler(
    private val activityResultRegistry: ActivityResultRegistry,
) {
    /**
     * Asks the signed-in user to grant [permissions] and suspends until they finish or back out.
     *
     * Permissions the user grants are persisted before this returns, so a later
     * [YouVersionApi.hasPermission] reflects them without the caller doing anything.
     *
     * @param permissions The permissions to ask for.
     * @return How the flow ended, and which permissions were granted.
     * @throws [YouVersionNetworkException] if no user is signed in, or the token request fails.
     */
    suspend fun requestDataExchange(permissions: Set<SignInWithYouVersionPermission>): DataExchangeResult {
        // dataExchangeToken throws MISSING_AUTHENTICATION when signed out or unconfigured, so the app key it
        // returns alongside is safe to read here.
        val token = YouVersionApi.dataExchange.dataExchangeToken(permissions)
        val appKey =
            YouVersionPlatformConfiguration.appKey
                ?: throw YouVersionNetworkException(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION)
        val permissionPageUrl = DataExchangeEndpoints.dataExchangeUrl(token = token.token, appKey = appKey)

        var launcher: ActivityResultLauncher<Intent>? = null
        val callbackIntent =
            try {
                suspendCancellableCoroutine { continuation ->
                    launcher =
                        activityResultRegistry.register(
                            "youversion-data-exchange",
                            ActivityResultContracts.StartActivityForResult(),
                        ) { result ->
                            continuation.resume(result.data)
                        }

                    AuthTabIntent.Builder().build().launch(
                        launcher,
                        permissionPageUrl.toUri(),
                        YouVersionPlatformConfiguration.authCallback,
                    )
                }
            } finally {
                launcher?.unregister()
            }

        val result =
            callbackIntent?.data?.let { dataExchangeResult(it) }
                ?: DataExchangeResult(status = DataExchangeStatus.Cancelled, grantedPermissions = emptyList())

        if (result.isGranted && result.grantedPermissions.isNotEmpty()) {
            YouVersionPlatformConfiguration.saveGrantedPermissions(result.grantedPermissions)
        }

        return result
    }
}
