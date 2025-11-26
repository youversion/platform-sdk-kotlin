package com.youversion.platform.core.organizations.api

import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parseApiBody
import com.youversion.platform.core.api.parseApiResponse
import com.youversion.platform.core.organizations.models.Organization
import com.youversion.platform.core.utilities.koin.YouVersionPlatformComponent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.path

object OrganizationsEndpoints : OrganizationsApi {
    private val httpClient: HttpClient
        get() = YouVersionPlatformComponent.httpClient

    // ----- Organization URLs
    fun organizationsUrl(bibleVersionId: Int): String =
        buildYouVersionUrlString {
            path("/v1/organizations")
            parameter("bible_id", bibleVersionId)
        }

    fun organizationUrl(organizationId: String): String =
        buildYouVersionUrlString {
            path("/v1/organizations/$organizationId")
        }

    // ----- Organization APIs
    override suspend fun organizations(bibleVersionId: Int): List<Organization> =
        httpClient
            .get(organizationsUrl(bibleVersionId))
            .let { parseApiResponse(it) }

    override suspend fun organization(organizationId: String): Organization =
        httpClient
            .get(organizationUrl(organizationId))
            .let { parseApiBody(it) }
}
