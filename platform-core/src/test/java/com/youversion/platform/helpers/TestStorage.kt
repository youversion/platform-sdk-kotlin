package com.youversion.platform.helpers

import com.youversion.platform.core.domain.Storage

class TestStorage : Storage {
    private val stringPrefs: MutableMap<String, String> = mutableMapOf()
    private val intPrefs: MutableMap<String, Int> = mutableMapOf()
    private val floatPrefs: MutableMap<String, Float> = mutableMapOf()
    private val longPrefs: MutableMap<String, Long> = mutableMapOf()

    override fun putString(
        key: String,
        value: String?,
    ) = putPref(stringPrefs, key, value)

    override fun getStringOrNull(key: String): String? = stringPrefs[key]

    override fun putInt(
        key: String,
        value: Int?,
    ) = putPref(intPrefs, key, value)

    override fun getIntOrNull(key: String): Int? = intPrefs[key]

    override fun putFloat(
        key: String,
        value: Float?,
    ) = putPref(floatPrefs, key, value)

    override fun getFloatOrNull(key: String): Float? = floatPrefs[key]

    // ----- Private Helpers
    private fun <T> putPref(
        pref: MutableMap<String, T>,
        key: String,
        value: T?,
    ) {
        value?.let { pref[key] = it }
            ?: pref.remove(key)
    }
}
