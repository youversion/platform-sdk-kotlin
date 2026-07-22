package com.youversion.platform.core.dataexchange.api

import co.touchlab.kermit.Logger
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.cannotDownload
import com.youversion.platform.core.api.notPermitted
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.dataexchange.models.DataExchangeToken
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

object DataExchangeEndpoints : DataExchangeApi {
    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

    // ----- Data Exchange URLs

    /**
     * The URL that mints a short-lived data exchange token.
     *
     * Note the app key is passed as `app-key` here but as `app_key` on [dataExchangeUrl]; the two spellings mirror
     * the server's own inconsistency rather than correcting it.
     */
    private fun dataExchangeTokenUrl(appKey: String): String =
        buildYouVersionUrlString {
            path("/data-exchange/token")
            parameter("app-key", appKey)
        }

    /** The permission page the user is sent to, authorized by [token]. */
    fun dataExchangeUrl(
        token: String,
        appKey: String,
    ): String =
        buildYouVersionUrlString {
            path("/data-exchange")
            parameter("token", token)
            parameter("app_key", appKey)
        }

    // ----- Data Exchange API
    override suspend fun dataExchangeToken(permissions: Set<SignInWithYouVersionPermission>): DataExchangeToken {
        if (YouVersionPlatformConfiguration.accessToken == null) {
            throw YouVersionNetworkException(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION)
        }
        val appKey =
            YouVersionPlatformConfiguration.appKey
                ?: throw YouVersionNetworkException(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION)

        val response =
            httpClient.post(dataExchangeTokenUrl(appKey)) {
                contentType(ContentType.Application.Json)
                setBody(tokenRequestBody(permissions))
            }

        if (response.status == HttpStatusCode.Unauthorized) {
            Logger.w { "Data exchange token request was not permitted for the signed-in user" }
            throw notPermitted()
        }

        if (response.status != HttpStatusCode.Created) {
            Logger.w { "Data exchange token request failed with status ${response.status.value}" }
            throw cannotDownload()
        }

        return response.body()
    }

    private fun tokenRequestBody(permissions: Set<SignInWithYouVersionPermission>): JsonObject =
        buildJsonObject {
            putJsonArray("permissions") {
                permissions.map { it.rawValue }.sorted().forEach { add(it) }
            }
        }
}
