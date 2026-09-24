package com.youversion.platform.core.languages.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.languages.models.Language

interface LanguagesApi {
    /**
     * Retrieves a list of languages supported in the Platform.
     *
     * This function fetches language overviews from the YouVersion Platform API.
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * @param country An optional country code for filtering languages. If provided, only languages
     *     used in that country will be returned.
     * @param languageRanges Optional BCP 47 language ranges used to negotiate the language of the response,
     *     most preferred first, such as `["zh-Hant-TW", "zh-Hant"]`. Sent as `Accept-Language`, with a
     *     descending quality value on every range after the first; only the first ten ranges are sent. An
     *     empty list leaves the header off and lets the server pick its default.
     * @return A list of [Language]s representing the available languages.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun languages(
        country: String? = null,
        fields: List<String>? = null,
        perPage: Int? = null,
        pageToken: String? = null,
        languageRanges: List<String> = emptyList(),
    ): PaginatedResponse<Language>
}
