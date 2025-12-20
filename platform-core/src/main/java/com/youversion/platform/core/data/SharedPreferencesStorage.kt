package com.youversion.platform.core.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.youversion.platform.core.domain.Storage

class SharedPreferencesStorage(
    context: Context,
) : Storage {
    companion object {
        private const val PREF_NAME = "com.youversion.platform.configuration_preferences"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun putString(
        key: String,
        value: String?,
    ) = prefs.edit {
        value?.let { putString(key, it) }
            ?: remove(key)
    }

    override fun getStringOrNull(key: String): String? = prefs.getString(key, null)

    override fun putInt(
        key: String,
        value: Int?,
    ) = prefs.edit {
        value?.let { putInt(key, it) }
            ?: remove(key)
    }

    override fun getIntOrNull(key: String): Int? =
        prefs
            .getInt(key, -1)
            .takeIf { it != -1 }

    override fun putFloat(
        key: String,
        value: Float?,
    ) = prefs.edit {
        value?.let { putFloat(key, it) }
            ?: remove(key)
    }

    override fun getFloatOrNull(key: String): Float? =
        prefs
            .getFloat(key, -1f)
            .takeIf { it != -1f }

    override fun putLong(
        key: String,
        value: Long?,
    ) = prefs.edit {
        value?.let { putLong(key, it) }
            ?: remove(key)
    }

    override fun getLongOrNull(key: String): Long? =
        prefs
            .getLong(key, -1L)
            .takeIf { it != -1L }
}
