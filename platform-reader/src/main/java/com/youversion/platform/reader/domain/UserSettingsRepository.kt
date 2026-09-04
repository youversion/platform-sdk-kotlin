package com.youversion.platform.reader.domain

import com.youversion.platform.core.domain.Storage

class UserSettingsRepository(
    private val storage: Storage,
) {
    companion object {
        private const val KEY_BIBLE_READER_THEME = "bible-reader-view--theme"
        private const val KEY_BIBLE_READER_FONT_SIZE = "bible-reader-view--font-size"
        private const val KEY_BIBLE_READER_FONT_FAMILY_NAME = "bible-reader-view--font-family-name"

        // Values under the old "--line-spacing" key were whole line-height multipliers; this one
        // holds the extra leading, so it gets a key of its own rather than a conversion on read.
        private const val KEY_BIBLE_READER_LINE_SPACING_FRACTION =
            "bible-reader-view--line-spacing-fraction"
    }

    var readerThemeId: Int?
        get() = storage.getIntOrNull(KEY_BIBLE_READER_THEME)
        set(value) = storage.putInt(KEY_BIBLE_READER_THEME, value)

    var readerFontSize: Float?
        get() = storage.getFloatOrNull(KEY_BIBLE_READER_FONT_SIZE)
        set(value) = storage.putFloat(KEY_BIBLE_READER_FONT_SIZE, value)

    var readerFontFamilyName: String?
        get() = storage.getStringOrNull(KEY_BIBLE_READER_FONT_FAMILY_NAME)
        set(value) = storage.putString(KEY_BIBLE_READER_FONT_FAMILY_NAME, value)

    var readerLineSpacingFraction: Float?
        get() = storage.getFloatOrNull(KEY_BIBLE_READER_LINE_SPACING_FRACTION)
        set(value) = storage.putFloat(KEY_BIBLE_READER_LINE_SPACING_FRACTION, value)
}
