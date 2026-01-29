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
import io.ktor.http.HttpStatusCode
import io.ktor.http.path

object LanguagesEndpoints : LanguagesApi {
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
    ): PaginatedResponse<Language> =
        httpClient
            .get(languagesUrl(country, fields, perPage, pageToken))
            .let {
                when (it.status) {
                    HttpStatusCode.NoContent -> PaginatedResponse(emptyList())
                    else -> parsePaginatedResponse(it)
                }
            }
}
