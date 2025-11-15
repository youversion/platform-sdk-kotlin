package com.youversion.platform.core.utilities.dependencies

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.youversion.platform.core.bibles.domain.BibleReference
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.util.Date

interface Store {
    var installId: String?

    var accessToken: String?

    var refreshToken: String?

    var expiryDate: Date?

    var bibleReference: BibleReference?

    var myVersionIds: Set<Int>?

    companion object {
        internal const val KEY_ACCESS_TOKEN = "YouVersionPlatformAccessToken"
        internal const val KEY_REFRESH_TOKEN = "YouVersionPlatformRefreshToken"
        internal const val KEY_EXPIRY_DATE = "YouVersionPlatformExpiryDate"
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

    override var refreshToken: String?
        get() = prefs.getString(Store.KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit { putString(Store.KEY_REFRESH_TOKEN, value) }

    override var expiryDate: Date?
        get() = prefs.getString(Store.KEY_EXPIRY_DATE, null)?.let { Json.decodeFromString(DateSerializer, it) }
        set(
            value,
        ) = prefs.edit { putString(Store.KEY_EXPIRY_DATE, value?.let { Json.encodeToString(DateSerializer, it) }) }

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

object DateSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun serialize(
        encoder: Encoder,
        value: Date,
    ) {
        encoder.encodeLong(value.time)
    }

    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}
