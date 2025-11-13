package com.youversion.platform.core.highlights.api

import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.ApiResponse
import com.youversion.platform.core.api.buildYouVersionUrl
import com.youversion.platform.core.api.invalidResponse
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.highlights.models.Highlight
import com.youversion.platform.core.utilities.koin.YouVersionPlatformComponent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object HighlightsEndpoints : HighlightsApi {
    private val httpClient: HttpClient
        get() = YouVersionPlatformComponent.httpClient

    // ----- Highlights URLs
    fun highlightsUrl(
        versionId: Int? = null,
        passageId: String? = null,
    ): Url =
        buildYouVersionUrl {
            path("/v1/highlights")
            versionId?.let { parameter("version_id", versionId) }
            passageId?.let { parameter("passage_id", passageId) }
        }

    fun highlightsDeleteUrl(
        versionId: Int,
        passageId: String,
    ): Url =
        buildYouVersionUrl {
            path("/v1/highlights/$passageId")
            parameter("version_id", versionId)
        }

    // ----- Highlights API
    override suspend fun createHighlight(
        versionId: Int,
        passageId: String,
        color: String,
    ): Boolean =
        httpClient
            .post(highlightsUrl()) {
                contentType(ContentType.Application.Json)
                buildJsonObject {
                    put("version_id", versionId)
                    put("passage_id", passageId)
                    put("color", color.lowercase())
                }.also { setBody(it) }
            }.status
            .isSuccess()

    override suspend fun highlights(
        versionId: Int,
        passageId: String,
    ): List<Highlight> {
        val response = httpClient.get(highlightsUrl(versionId, passageId))

        if (response.status == HttpStatusCode.Unauthorized) {
            Logger.w { "error 401: unauthorized. Check your appKey" }
            return emptyList()
        }

        if (response.status == HttpStatusCode.Forbidden) {
            Logger.w { "error 403: forbidden. Check your appKey and it's entitlements" }
            return emptyList()
        }

        if (!response.status.isSuccess()) {
            Logger.w { "Request failed with status code ${response.status.value} " }
            return emptyList()
        }

        if (response.status == HttpStatusCode.NoContent) {
            return emptyList()
        }

        try {
            return response.body<ApiResponse<List<Highlight>>>().data
        } catch (e: Exception) {
            throw invalidResponse(e)
        }
    }

    override suspend fun updateHighlight(
        versionId: Int,
        passageId: String,
        color: String,
    ): Boolean =
        httpClient
            .put {
                highlightsUrl()
                contentType(ContentType.Application.Json)
                buildJsonObject {
                    put("version_id", versionId)
                    put("passage_id", passageId)
                    put("color", color.lowercase())
                }.also { setBody(it) }
            }.status
            .isSuccess()

    override suspend fun deleteHighlight(
        versionId: Int,
        passageId: String,
    ): Boolean =
        httpClient
            .delete(highlightsDeleteUrl(versionId, passageId)) {
                contentType(ContentType.Application.Json)
            }.status
            .isSuccess()
}
