package com.youversion.platform.core.highlights.api

import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.notPermitted
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parseApiResponse
import com.youversion.platform.core.highlights.models.Highlight
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.UUID

object HighlightsEndpoints : HighlightsApi {
    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

    // ----- Highlights URLs
    fun highlightsUrl(
        versionId: Int? = null,
        passageId: String? = null,
    ): String =
        buildYouVersionUrlString {
            path("/v1/highlights")
            versionId?.let { parameter("bible_id", versionId) }
            passageId?.let { parameter("passage_id", passageId) }
        }

    fun highlightsDeleteUrl(
        versionId: Int,
        passageId: String,
    ): String =
        buildYouVersionUrlString {
            path("/v1/highlights/$passageId")
            parameter("bible_id", versionId)
        }

    // ----- Highlights API
    override suspend fun createHighlight(
        versionId: Int,
        passageId: String,
        color: String,
    ): Boolean =
        sendHighlightChange {
            httpClient.post(highlightsUrl()) {
                contentType(ContentType.Application.Json)
                setBody(highlightRequestBody(versionId, passageId, color))
            }
        }

    /**
     * Reads the chapter's highlights, throwing on any response that does not carry the server's answer rather than
     * reporting it as an empty chapter. An empty list is indistinguishable from the server saying the chapter holds no
     * highlights, and callers merge that answer as authoritative — so returning one would have the cache discard
     * highlights and throttle the next reload on a response that never arrived.
     *
     * `403 Forbidden` is reported as [com.youversion.platform.core.api.YouVersionNetworkException.Reason.NOT_PERMITTED]:
     * the user has not granted this app access to their highlights. That is true of the whole account rather than of
     * this chapter, and no retry changes it.
     *
     * `401 Unauthorized` is deliberately *not* reported that way, but as
     * [com.youversion.platform.core.api.YouVersionNetworkException.Reason.MISSING_AUTHENTICATION]. Unlike the
     * app-scoped reads that share [com.youversion.platform.core.api.parseApiBody], this endpoint authenticates the user
     * with a bearer token, so a 401 means they are signed out or their access token expired rather than that the app
     * key is wrong. Signing out already clears the cached highlights on its own, so treating a 401 as a withdrawal of
     * access would gain nothing there while letting a token expiring mid-session discard the highlights of a user who
     * is still signed in. It is therefore recoverable, the same distinction [sendHighlightChange] draws on the write
     * path.
     */
    override suspend fun highlights(
        versionId: Int,
        passageId: String,
    ): List<Highlight> {
        val response = httpClient.get(highlightsUrl(versionId, passageId))

        if (response.status == HttpStatusCode.Unauthorized) {
            Logger.w { "error 401: unauthorized. The user is signed out or their access token has expired" }
            throw YouVersionNetworkException(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION)
        }

        if (response.status == HttpStatusCode.NoContent) {
            return emptyList()
        }

        return parseApiResponse(response)
    }

    override suspend fun updateHighlight(
        versionId: Int,
        passageId: String,
        color: String,
    ): Boolean =
        sendHighlightChange {
            httpClient.put(highlightsUrl()) {
                contentType(ContentType.Application.Json)
                setBody(highlightRequestBody(versionId, passageId, color))
            }
        }

    /**
     * Sends a change to a highlight — creating, recoloring, or removing one — and reports whether it succeeded,
     * throwing instead when the server refused it because the user has not granted permission to change highlights.
     *
     * Only `403 Forbidden` is treated that way. A `401 Unauthorized` means the request was not authenticated, which a
     * token refresh may resolve, so it is reported as an ordinary failure and stays on the retry path — otherwise an
     * access token that expired mid-request would discard the user's highlight.
     */
    private suspend fun sendHighlightChange(request: suspend () -> HttpResponse): Boolean {
        val response = request()
        if (response.status == HttpStatusCode.Forbidden) {
            Logger.w { "Highlight write forbidden: the user has not granted permission to change highlights" }
            throw notPermitted()
        }
        return response.status.isSuccess()
    }

    private fun highlightRequestBody(
        versionId: Int,
        passageId: String,
        color: String,
    ): JsonObject =
        buildJsonObject {
            put("request_id", UUID.randomUUID().toString())
            putJsonObject("highlight") {
                put("bible_id", versionId)
                put("passage_id", passageId)
                put("color", color.lowercase())
            }
        }

    override suspend fun deleteHighlight(
        versionId: Int,
        passageId: String,
    ): Boolean =
        sendHighlightChange {
            httpClient.delete(highlightsDeleteUrl(versionId, passageId)) {
                contentType(ContentType.Application.Json)
            }
        }
}
