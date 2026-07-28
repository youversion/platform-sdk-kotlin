package com.youversion.platform.ui.signin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.ui.dataexchange.dataExchangeResult
import kotlin.getValue

abstract class SignInWithYouVersionActivity : ComponentActivity() {
    private val signInViewModel by viewModels<SignInViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (YouVersionAuthentication.isAuthenticationInProgress(this)) {
            YouVersionAuthentication.cancelAuthentication(this)
        }
    }

    private fun handleIntent(intent: Intent?) {
        val callback = intent?.data ?: return
        // Sign-in and data exchange arrive on the same youversionauth://callback deep link. A data exchange callback
        // carries data_exchange_status; persist any granted permission and let observers of the configuration react.
        // Anything else is a sign-in callback.
        val dataExchangeStatus =
            try {
                callback.getQueryParameter(DATA_EXCHANGE_STATUS_PARAMETER)
            } catch (error: Exception) {
                Log.w("YouVersionDataExchange", "Ignoring an unreadable callback deep link", error)
                return
            }
        if (dataExchangeStatus != null) {
            val result = dataExchangeResult(callback)
            if (result.isGranted && result.grantedPermissions.isNotEmpty()) {
                try {
                    YouVersionPlatformConfiguration.saveGrantedPermissions(result.grantedPermissions)
                } catch (error: Exception) {
                    Log.w("YouVersionDataExchange", "Could not persist the granted permissions", error)
                }
            }
        } else {
            signInViewModel.onAction(SignInViewModel.Action.ProcessAuthCallback(intent))
        }
    }

    private companion object {
        const val DATA_EXCHANGE_STATUS_PARAMETER = "data_exchange_status"
    }
}
