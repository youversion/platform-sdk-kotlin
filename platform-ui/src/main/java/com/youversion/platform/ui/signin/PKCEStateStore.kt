package com.youversion.platform.ui.signin

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * A utility to store and retrieve PKCE parameters across application
 * sessions and process death. This is needed for the authentication flow.
 */
internal object PKCEStateStore {
    private const val PREF_NAME = "youversion_pkce_state_prefs"
    private const val KEY_CODE_VERIFIER = "pkce_code_verifier"
    private const val KEY_STATE = "pkce_state"

    private var prefs: SharedPreferences? = null

    private fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
        return prefs!!
    }

    /**
     * Saves the code verifier and state to SharedPreferences.
     *
     * @param context The Android context.
     * @param codeVerifier The code verifier generated for the PKCE flow.
     * @param state The unique state string generated for the PKCE flow.
     */
    fun save(
        context: Context,
        codeVerifier: String,
        state: String,
    ) {
        getPrefs(context)
            .edit {
                putString(KEY_CODE_VERIFIER, codeVerifier)
                putString(KEY_STATE, state)
            }
    }

    /**
     * Retrieves the stored code verifier.
     *
     * @param context The Android context.
     * @return The stored code verifier, or null if not found.
     */
    fun getCodeVerifier(context: Context): String? = getPrefs(context).getString(KEY_CODE_VERIFIER, null)

    /**
     * Retrieves the stored state.
     *
     * @param context The Android context.
     * @return The stored state string, or null if not found.
     */
    fun getState(context: Context): String? = getPrefs(context).getString(KEY_STATE, null)

    /**
     * Clears the stored PKCE parameters from SharedPreferences. This should be
     * called after the authentication flow is completed or cancelled.
     *
     * @param context The Android context.
     */
    fun clear(context: Context) {
        getPrefs(context)
            .edit {
                remove(KEY_CODE_VERIFIER)
                remove(KEY_STATE)
            }
    }
}
