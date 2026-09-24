package com.youversion.platform.core.languages.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.fields
import com.youversion.platform.core.api.pageSize
import com.youversion.platform.core.api.pageToken
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parsePaginatedResponse
import com.youversion.platform.core.languages.models.Language
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.path

internal object LanguagesEndpoints : LanguagesApi {
    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

    fun languagesUrl(
        country: String? = null,
        fields: List<String>? = null,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): String =
        buildYouVersionUrlString {
            path("/v1/languages")
            parameter("country", country)
            fields(fields)
            pageSize(pageSize, fields)
            pageToken(pageToken)
        }

    override suspend fun languages(
        country: String?,
        fields: List<String>?,
        perPage: Int?,
        pageToken: String?,
        languageRanges: List<String>,
    ): PaginatedResponse<Language> =
        httpClient
            .get(languagesUrl(country, fields, perPage, pageToken)) {
                acceptLanguage(languageRanges)?.let { header(HttpHeaders.AcceptLanguage, it) }
            }.let {
                when (it.status) {
                    HttpStatusCode.NoContent -> PaginatedResponse(emptyList())
                    else -> parsePaginatedResponse(it)
                }
            }

    private fun acceptLanguage(languageRanges: List<String>): String? =
        languageRanges
            .filter { it.isNotBlank() }
            .take(MAX_RANGES)
            .mapIndexed { index, range ->
                when (index) {
                    0 -> range
                    else -> "$range;q=0.${MAX_RANGES - index}"
                }
            }.joinToString(", ")
            .takeIf { it.isNotEmpty() }
}

/** Ranges past the tenth have no distinct quality value left below them, so they are dropped. */
private const val MAX_RANGES = 10
