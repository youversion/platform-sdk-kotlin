package com.youversion.platform.core.languages.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.pageSize
import com.youversion.platform.core.api.pageToken
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parsePaginatedResponse
import com.youversion.platform.core.languages.models.Language
import com.youversion.platform.core.utilities.koin.YouVersionPlatformComponent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.path

object LanguagesEndpoints : LanguagesApi {
    private val httpClient: HttpClient
        get() = YouVersionPlatformComponent.httpClient

    fun languagesUrl(
        country: String? = null,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): String =
        buildYouVersionUrlString {
            path("/v1/languages")
            parameter("country", country)
            pageSize(pageSize)
            pageToken(pageToken)
        }

    override suspend fun languages(
        country: String?,
        perPage: Int?,
        pageToken: String?,
    ): PaginatedResponse<Language> =
        httpClient
            .get(languagesUrl(country, perPage, pageToken))
            .let {
                when (it.status) {
                    HttpStatusCode.NoContent -> PaginatedResponse(emptyList())
                    else -> parsePaginatedResponse(it)
                }
            }
}
