package com.youversion.platform.core.languages.domain

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.fetchAllPages
import com.youversion.platform.core.languages.models.Language

/**
 * Responsible for fetching, caching, and retrieving languages
 */
class LanguageRepository {
    /**
     * Returns a list of languages based on the given country.
     */
    suspend fun suggestedLanguages(country: String): List<Language> =
        fetchAllPages { nextPageToken ->
            YouVersionApi.languages.languages(country = country, perPage = 99, pageToken = nextPageToken)
        }

    /**
     * Returns a list of all languages, paginated
     */
    suspend fun languages(): List<Language> {
        TODO()
    }
}
