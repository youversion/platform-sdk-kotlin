package com.youversion.platform.core.languages.api

import com.youversion.platform.core.api.buildYouVersionUrl
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parseApiResponse
import com.youversion.platform.core.languages.models.Language
import com.youversion.platform.core.utilities.koin.YouVersionPlatformComponent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.path

object LanguagesEndpoints : LanguagesApi {
    private val httpClient: HttpClient
        get() = YouVersionPlatformComponent.httpClient

    fun languagesUrl(country: String? = null): Url =
        buildYouVersionUrl {
            path("/v1/languages")
            country?.let { parameter("country", country) }
        }

    override suspend fun languages(country: String?): List<Language> =
        httpClient
            .get(languagesUrl(country))
            .let {
                when (it.status) {
                    HttpStatusCode.NoContent -> emptyList()
                    else -> parseApiResponse(it)
                }
            }
}
