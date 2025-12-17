package com.youversion.platform.reader.screens.languages

import com.youversion.platform.core.languages.models.Language

data class LanguageRowItem(
    val language: Language,
    val displayName: String,
    val localeDisplayName: String?,
) {
    constructor(language: Language, langCode: String) : this(
        language = language,
        displayName = language.displayNames[language.id] ?: language.id,
        localeDisplayName = language.displayNames[langCode],
    )
}
