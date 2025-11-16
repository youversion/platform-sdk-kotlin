package com.youversion.platform.core.utilities.dependencies

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.youversion.platform.core.bibles.domain.BibleReference
import kotlinx.serialization.json.Json

interface Store {
    var installId: String?

    var accessToken: String?

    var bibleReference: BibleReference?

    var myVersionIds: Set<Int>?

    companion object {
        internal const val KEY_ACCESS_TOKEN = "YouVersionPlatformAccessToken"
        internal const val KEY_INSTALL_ID = "YouVersionPlatformInstallID"
        internal const val KEY_BIBLE_READER_REFERENCE = "bible-reader-view--reference"
        internal const val KEY_BIBLE_READER_MY_VERSIONS = "bible-reader-view--my-versions"
    }
}

class SharedPreferencesStore(
    context: Context,
) : Store {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override var installId: String?
        get() = prefs.getString(Store.KEY_INSTALL_ID, null)
        set(value) = prefs.edit { putString(Store.KEY_INSTALL_ID, value) }

    override var accessToken: String?
        get() = prefs.getString(Store.KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit { putString(Store.KEY_ACCESS_TOKEN, value) }

    override var bibleReference: BibleReference?
        get() = prefs.getString(Store.KEY_BIBLE_READER_REFERENCE, null)?.let { Json.decodeFromString(it) }
        set(value) = prefs.edit { putString(Store.KEY_BIBLE_READER_REFERENCE, Json.encodeToString(value)) }

    override var myVersionIds: Set<Int>?
        get() = prefs.getStringSet(Store.KEY_BIBLE_READER_MY_VERSIONS, emptySet())?.map { it.toInt() }?.toSet()
        set(
            value,
        ) = prefs.edit { putStringSet(Store.KEY_BIBLE_READER_MY_VERSIONS, value?.map { it.toString() }?.toSet()) }

    companion object {
        private const val PREF_NAME = "com.youversion.platform.configuration_preferences"
    }
}
