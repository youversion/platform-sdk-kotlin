package com.youversion.platform.core.languages.domain

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.languages.models.Language

/**
 * Responsible for fetching, caching, and retrieving languages
 */
class LanguageRepository {
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
}
