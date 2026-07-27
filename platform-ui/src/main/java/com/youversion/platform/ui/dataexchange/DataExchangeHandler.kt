package com.youversion.platform.ui.dataexchange

import android.content.Intent
import android.util.Log
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
import kotlinx.coroutines.CancellationException
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
     * Permissions the user grants are persisted as soon as the callback carrying them arrives — before the host
     * activity resumes, and so before this returns — meaning a later [YouVersionApi.hasPermission] reflects them
     * without the caller doing anything.
     *
     * @param permissions The permissions to ask for.
     * @return How the flow ended, and which permissions were granted. A failed token request or browser session is
     *         reported as a cancellation rather than thrown, so the flow has a single, non-crashing failure shape.
     */
    suspend fun requestDataExchange(permissions: Set<SignInWithYouVersionPermission>): DataExchangeResult =
        try {
            exchange(permissions)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.w("YouVersionDataExchange", "Data exchange flow failed; treating as cancelled", error)
            cancelledResult()
        }

    private suspend fun exchange(permissions: Set<SignInWithYouVersionPermission>): DataExchangeResult {
        val token = YouVersionApi.dataExchange.dataExchangeToken(permissions)
        val appKey =
            YouVersionPlatformConfiguration.appKey
                ?: throw YouVersionNetworkException(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION)
        val permissionPageUrl = DataExchangeEndpoints.dataExchangeUrl(token = token.token, appKey = appKey)

        var launcher: ActivityResultLauncher<Intent>? = null
        return try {
            suspendCancellableCoroutine { continuation ->
                launcher =
                    activityResultRegistry.register(
                        "youversion-data-exchange",
                        ActivityResultContracts.StartActivityForResult(),
                    ) { activityResult ->
                        val result = activityResult.data?.data?.let { dataExchangeResult(it) } ?: cancelledResult()
                        persistGrantedPermissions(result)
                        continuation.resume(result)
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
    }

    /**
     * Persists whatever [result] granted, while the activity result carrying it is still being dispatched.
     *
     * Android delivers an activity result before the host activity resumes, but resuming this flow's continuation is
     * dispatched rather than immediate, so persisting after the continuation resumes would land *after* a caller
     * reading the permission on resume. Writing here keeps the permission settled by the time anything can observe
     * it, matching the deep link route, which persists from onNewIntent. Failure is logged rather than thrown, since
     * throwing during an activity result dispatch would crash the host app.
     */
    private fun persistGrantedPermissions(result: DataExchangeResult) {
        if (!result.isGranted || result.grantedPermissions.isEmpty()) return
        try {
            YouVersionPlatformConfiguration.saveGrantedPermissions(result.grantedPermissions)
        } catch (error: Exception) {
            Log.w("YouVersionDataExchange", "Could not persist the granted permissions", error)
        }
    }

    private fun cancelledResult() =
        DataExchangeResult(status = DataExchangeStatus.Cancelled, grantedPermissions = emptyList())
}
