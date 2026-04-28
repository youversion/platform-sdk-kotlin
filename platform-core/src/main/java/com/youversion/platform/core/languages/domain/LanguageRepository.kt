package com.youversion.platform.core.languages.domain

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.languages.models.Language
import java.util.Locale

/**
 * Responsible for fetching, caching, and retrieving languages
 */
class LanguageRepository(
    private val bibleVersionRepository: BibleVersionRepository,
) {
    val localeCountryCode: String
        get() = Locale.getDefault().country ?: "US"
    val localeLanguageCode: String
        get() = Locale.getDefault().language ?: "en"

    /**
     * Returns a list of languages based on the given country.
     */
    suspend fun suggestedLanguages(country: String): List<Language> =
        YouVersionApi
            .languages
            .languages(
                country = country,
                fields = listOf(Language.CodingKey.LANGUAGE, Language.CodingKey.DISPLAY_NAMES),
            ).data

    /**
     * Returns a list of all languages, paginated
     */
    suspend fun languages(): List<Language> =
        YouVersionApi
            .languages
            .languages(
                fields = listOf(Language.CodingKey.LANGUAGE, Language.CodingKey.DISPLAY_NAMES),
            ).data

    val allPermittedLanguageTags: List<String>
        get() =
            bibleVersionRepository.permittedVersions
                ?.mapNotNull { it.languageTag }
                ?.distinct()
                ?: emptyList()

    private var suggestedLanguageTags: List<String>? = null

    suspend fun suggestedLanguageTags(): List<String> {
        suggestedLanguageTags?.let { return it }

        val data = suggestedLanguages(localeCountryCode)
        val codes = if (data.isEmpty()) listOf("en", "es") else extractLanguageCodes(data)

        val result =
            bibleVersionRepository.permittedVersions?.let { permittedVersions ->
                codes
                    .filter { languageCode ->
                        permittedVersions.isEmpty() ||
                            permittedVersions.any { it.languageTag == languageCode }
                    }
            } ?: codes
        suggestedLanguageTags = result
        return result
    }

    private fun extractLanguageCodes(languages: List<Language>): List<String> =
        languages
            .mapNotNull { it.language }
            .distinct()

    private var languageNames: Map<String, String> = emptyMap()

    suspend fun loadLanguageNames(version: BibleVersion?) {
        if (languageNames.isNotEmpty()) return

        val result = languages()

        val langNames =
            result
                .mapNotNull { language ->
                    language.language?.let { code ->
                        language.displayNames?.let { displayNames ->
                            bestDisplayName(displayNames, version)?.let { name ->
                                code to name
                            }
                        }
                    }
                }.toMap()

        languageNames = langNames
    }

    fun languageName(lang: String): String {
        languageNames[lang]?.let { return it }
        val displayName = Locale.forLanguageTag(lang).getDisplayLanguage(Locale.getDefault())
        return displayName.takeIf { it.isNotBlank() } ?: lang
    }

    private fun bestDisplayName(
        names: Map<String, String?>,
        version: BibleVersion?,
    ): String? {
        if (names.isEmpty()) return null
        if (names.size < 2) return names.entries.firstOrNull()?.value

        names[localeLanguageCode]?.let { return it }
        version?.languageTag?.let { names[it] }?.let { return it }
        names["en"]?.let { return it }

        return names.entries.firstOrNull()?.value
    }
}
