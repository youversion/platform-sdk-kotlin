package com.youversion.platform.core.api

import com.youversion.platform.core.api.YouVersionNetworkException.ValidationIssue
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.fromHttpToGmtDate
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Creates an exception for this error response, carrying its status, the server's message, `Retry-After`, and any
 * validation issues. The reason is [reason] when given, otherwise classified from the status. A body that cannot be
 * read leaves the server's details empty and becomes the cause.
 */
internal suspend fun HttpResponse.failureException(
    reason: YouVersionNetworkException.Reason? = null,
): YouVersionNetworkException {
    var readFailure: Exception? = null
    val body =
        try {
            ErrorBody.parse(bodyAsText(), contentType())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            readFailure = e
            ErrorBody.EMPTY
        }
    return YouVersionNetworkException.requestFailed(
        status = status,
        reason = reason,
        cause = readFailure,
        serverMessage = body.message,
        retryAfter = retryAfter(headers[HttpHeaders.RetryAfter]),
        validationIssues = body.validationIssues,
    )
}

/**
 * The details an error response body explains a failure with.
 */
internal class ErrorBody(
    val message: String?,
    val validationIssues: List<ValidationIssue>,
) {
    companion object {
        val EMPTY = ErrorBody(message = null, validationIssues = emptyList())

        private const val MAX_MESSAGE_LENGTH = 1_000

        /**
         * Reads a JSON error body, or uses a non-blank `text/plain` body as the message. Any other body is [EMPTY].
         * The message is trimmed and capped at [MAX_MESSAGE_LENGTH] characters.
         */
        fun parse(
            text: String,
            contentType: ContentType?,
        ): ErrorBody {
            val json =
                try {
                    Json.parseToJsonElement(text)
                } catch (_: Exception) {
                    null
                }
            if (json is JsonObject) return fromJson(json)

            val plainText = text.trim()
            return if (contentType?.match(ContentType.Text.Plain) == true && plainText.isNotEmpty()) {
                ErrorBody(message = plainText.take(MAX_MESSAGE_LENGTH), validationIssues = emptyList())
            } else {
                EMPTY
            }
        }

        private fun fromJson(json: JsonObject): ErrorBody {
            val detail = json["detail"]
            val issues =
                if (detail is JsonArray) {
                    detail.mapNotNull { issue ->
                        if (issue !is JsonObject) return@mapNotNull null
                        val message = issue["msg"].stringOrNull() ?: return@mapNotNull null
                        val location = issue["loc"]
                        ValidationIssue(
                            location =
                                if (location is JsonArray) {
                                    location.filterIsInstance<JsonPrimitive>().mapNotNull { it.contentOrNull }
                                } else {
                                    emptyList()
                                },
                            message = message,
                        )
                    }
                } else {
                    emptyList()
                }
            val fault = json["fault"]
            val message =
                json["message"].stringOrNull()
                    ?: (if (fault is JsonObject) fault["faultstring"].stringOrNull() else null)
                    ?: detail.stringOrNull()
                    ?: issues.firstOrNull()?.message
            return ErrorBody(message = message?.take(MAX_MESSAGE_LENGTH), validationIssues = issues)
        }

        private fun JsonElement?.stringOrNull(): String? =
            if (this is JsonPrimitive && isString) content.trim().takeIf { it.isNotEmpty() } else null
    }
}

/**
 * Reads a `Retry-After` header given in seconds or as an HTTP date, never negative. Returns `null` when [value] is
 * missing or malformed.
 */
internal fun retryAfter(
    value: String?,
    nowMillis: Long = System.currentTimeMillis(),
): Duration? {
    val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    trimmed.toLongOrNull()?.let { return it.coerceAtLeast(0).seconds }
    val dateMillis =
        try {
            trimmed.fromHttpToGmtDate().timestamp
        } catch (_: Exception) {
            return null
        }
    return (dateMillis - nowMillis).coerceAtLeast(0).milliseconds
}
