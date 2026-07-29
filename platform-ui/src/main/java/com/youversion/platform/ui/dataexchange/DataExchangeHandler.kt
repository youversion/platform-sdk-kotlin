package com.youversion.platform.ui.dataexchange

import android.content.Intent
import android.net.Uri
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
     * @param onBrowserOpened Called once the browser is open, and never called when it could not be opened. A caller
     *        that settles the flow on a resume needs this to tell a resume that follows the browser from one that
     *        arrives while the exchange token is still being requested, since only the first means the user answered.
     * @return How the flow ended, and which permissions were granted. A failure is reported rather than thrown, and
     *         everything that can fail here happens before the browser opens, so it is reported as
     *         [DataExchangeStatus.NotStarted] — telling a caller that settles the flow on a resume that no resume is
     *         coming.
     */
    suspend fun requestDataExchange(
        permissions: Set<SignInWithYouVersionPermission>,
        onBrowserOpened: () -> Unit = {},
    ): DataExchangeResult =
        try {
            exchange(permissions, onBrowserOpened)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.w("YouVersionDataExchange", "Data exchange flow could not be started", error)
            DataExchangeResult(status = DataExchangeStatus.NotStarted, grantedPermissions = emptyList())
        }

    private suspend fun exchange(
        permissions: Set<SignInWithYouVersionPermission>,
        onBrowserOpened: () -> Unit,
    ): DataExchangeResult {
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
                        val result = callbackResult(activityResult.data?.data)
                        persistGrantedPermissions(result)
                        continuation.resume(result)
                    }

                AuthTabIntent.Builder().build().launch(
                    launcher,
                    permissionPageUrl.toUri(),
                    YouVersionPlatformConfiguration.authCallback,
                )
                onBrowserOpened()
            }
        } finally {
            launcher?.unregister()
        }
    }

    /**
     * Reads [callbackUri] as a result, treating one that cannot be read as a cancellation. Runs during the activity
     * result dispatch, so it must not throw: reading query parameters fails on a URI that is not hierarchical, and
     * letting that escape would crash the host app and leave this flow suspended forever.
     */
    private fun callbackResult(callbackUri: Uri?): DataExchangeResult {
        if (callbackUri == null) return cancelledResult()
        return try {
            dataExchangeResult(callbackUri)
        } catch (error: Exception) {
            Log.w("YouVersionDataExchange", "Could not read the data exchange callback", error)
            cancelledResult()
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
