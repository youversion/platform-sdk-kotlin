package com.youversion.platform.core.utilities.dependencies

import android.content.Context
import android.content.SharedPreferences
import com.youversion.platform.core.bibles.domain.BibleReference
import kotlinx.serialization.json.Json

interface Store {
    var installId: String?

    var accessToken: String?

    var bibleReference: BibleReference?

    var readerThemeId: Int?
    var readerFontSize: Float?
    var readerLineSpacing: Float?
    var readerFontDefinitionName: String?

    var myVersionIds: Set<Int>?

    companion object {
        internal const val KEY_ACCESS_TOKEN = "YouVersionPlatformAccessToken"
        internal const val KEY_INSTALL_ID = "YouVersionPlatformInstallID"
        internal const val KEY_BIBLE_READER_REFERENCE = "bible-reader-view--reference"
        internal const val KEY_BIBLE_READER_MY_VERSIONS = "bible-reader-view--my-versions"
        internal const val KEY_BIBLE_READER_THEME = "bible-reader-view--theme"
        internal const val KEY_BIBLE_READER_LINE_SPACING = "bible-reader-view--line-spacing"
        internal const val KEY_BIBLE_READER_FONT_SIZE = "bible-reader-view--font-size"
        internal const val KEY_BIBLE_READER_FONT_FAMILY = "bible-reader-view--font-family"
    }
}

class SharedPreferencesStore(
    context: Context,
) : Store {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override var installId: String?
        get() = prefs.getString(Store.KEY_INSTALL_ID, null)
        set(value) = edit { putString(Store.KEY_INSTALL_ID, value) }

    override var accessToken: String?
        get() = prefs.getString(Store.KEY_ACCESS_TOKEN, null)
        set(value) = edit { putString(Store.KEY_ACCESS_TOKEN, value) }

    override var bibleReference: BibleReference?
        get() = prefs.getString(Store.KEY_BIBLE_READER_REFERENCE, null)?.let { Json.decodeFromString(it) }
        set(value) = edit { putString(Store.KEY_BIBLE_READER_REFERENCE, Json.encodeToString(value)) }

    override var readerThemeId: Int?
        get() = prefs.getInt(Store.KEY_BIBLE_READER_THEME, 1)
        set(value) =
            edit { value?.let { putInt(Store.KEY_BIBLE_READER_THEME, it) } ?: remove(Store.KEY_BIBLE_READER_THEME) }

    override var readerFontSize: Float?
        get() = prefs.getFloat(Store.KEY_BIBLE_READER_FONT_SIZE, -1f)
        set(value) =
            edit {
                value?.let { putFloat(Store.KEY_BIBLE_READER_FONT_SIZE, value) }
                    ?: remove(Store.KEY_BIBLE_READER_FONT_SIZE)
            }
    override var readerLineSpacing: Float?
        get() = prefs.getFloat(Store.KEY_BIBLE_READER_LINE_SPACING, -1f)
        set(value) =
            edit {
                value?.let { putFloat(Store.KEY_BIBLE_READER_LINE_SPACING, it) }
                    ?: remove(Store.KEY_BIBLE_READER_LINE_SPACING)
            }
    override var readerFontDefinitionName: String?
        get() = prefs.getString(Store.KEY_BIBLE_READER_FONT_FAMILY, null)
        set(value) =
            edit {
                value?.let { putString(Store.KEY_BIBLE_READER_FONT_FAMILY, it) }
                    ?: remove(Store.KEY_BIBLE_READER_FONT_FAMILY)
            }
    override var myVersionIds: Set<Int>?
        get() = prefs.getStringSet(Store.KEY_BIBLE_READER_MY_VERSIONS, emptySet())?.map { it.toInt() }?.toSet()
        set(
            value,
        ) = edit { putStringSet(Store.KEY_BIBLE_READER_MY_VERSIONS, value?.map { it.toString() }?.toSet()) }

    companion object {
        private const val PREF_NAME = "com.youversion.platform.configuration_preferences"
    }

    private inline fun edit(action: SharedPreferences.Editor.() -> Unit) =
        prefs
            .edit()
            .apply(action)
            .apply()
}
