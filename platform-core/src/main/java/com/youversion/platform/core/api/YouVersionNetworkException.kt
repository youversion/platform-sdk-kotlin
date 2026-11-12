package com.youversion.platform.core.api

import io.ktor.http.HttpStatusCode
import java.io.Serializable
import java.util.Collections
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

public class YouVersionNetworkException private constructor(
    public val reason: Reason,
    cause: Exception?,
    message: String,
    /**
     * The HTTP status of the error response, or `null` when the failure did not come from one.
     */
    public val statusCode: Int?,
    /**
     * The server's description of the failure, when the error response includes one.
     */
    public val serverMessage: String?,
    private val retryAfterMillis: Long?,
    validationIssues: List<ValidationIssue>,
) : Exception(message, cause) {
    /**
     * How long the server asks the app to wait before retrying, from the `Retry-After` header.
     */
    public val retryAfter: Duration?
        get() = retryAfterMillis?.milliseconds

    /**
     * The request parameters the server found invalid. Empty when it reports none.
     */
    public val validationIssues: List<ValidationIssue> = Collections.unmodifiableList(validationIssues.toList())

    /**
     * Creates an exception for [reason], with its name as the message.
     */
    public constructor(reason: Reason, cause: Exception? = null) : this(
        reason = reason,
        cause = cause,
        message = reason.toString(),
        statusCode = null,
        serverMessage = null,
        retryAfterMillis = null,
        validationIssues = emptyList(),
    )

    /**
     * Why a request failed. More values may be added in a minor release, so a `when` over [reason]
     * needs an `else` branch.
     */
    public class Reason private constructor(
        private val name: String,
    ) : Serializable {
        override fun toString(): String = name

        private fun readResolve(): Any =
            listOf(
                REQUEST_FAILED,
                RATE_LIMITED,
                SERVER_ERROR,
                REQUEST_REJECTED,
                NOT_PERMITTED,
                INVALID_RESPONSE,
                MISSING_AUTHENTICATION,
            ).firstOrNull { it.name == name } ?: this

        public companion object {
            /**
             * The server returned an error response the SDK does not classify more specifically.
             */
            @JvmField
            public val REQUEST_FAILED: Reason = Reason("request_failed")

            /**
             * The server returned `429 Too Many Requests`. The request may succeed if retried later.
             */
            @JvmField
            public val RATE_LIMITED: Reason = Reason("rate_limited")

            /**
             * The server returned a `5xx` status. The request may succeed if retried later.
             */
            @JvmField
            public val SERVER_ERROR: Reason = Reason("server_error")

            /**
             * The server rejected the request with a `4xx` status, such as for a resource the app
             * cannot access. Retrying the same request usually does not help. `429` is
             * [RATE_LIMITED], and `401` and `403` are reported as [MISSING_AUTHENTICATION] or
             * [NOT_PERMITTED].
             */
            @JvmField
            public val REQUEST_REJECTED: Reason = Reason("request_rejected")

            /**
             * The App Key is invalid or lacks permission, or the signed-in user has not granted the
             * permission the request requires. Retrying does not change either outcome.
             */
            @JvmField
            public val NOT_PERMITTED: Reason = Reason("not_permitted")

            /**
             * The response is not valid.
             */
            @JvmField
            public val INVALID_RESPONSE: Reason = Reason("invalid_response")

            /**
             * The request needs a signed-in user, and none is signed in or their access token is
             * not accepted.
             */
            @JvmField
            public val MISSING_AUTHENTICATION: Reason = Reason("missing_authentication")
        }
    }

    /**
     * A request parameter the server found invalid.
     */
    public class ValidationIssue internal constructor(
        location: List<String>,
        /**
         * Why the value is invalid.
         */
        public val message: String,
    ) : Serializable {
        /**
         * The path to the invalid value, such as `["query", "language_ranges"]`.
         */
        public val location: List<String> = Collections.unmodifiableList(location.toList())

        override fun equals(other: Any?): Boolean =
            other is ValidationIssue && location == other.location && message == other.message

        override fun hashCode(): Int = 31 * location.hashCode() + message.hashCode()

        override fun toString(): String = "${location.joinToString(".")}: $message"
    }

    internal companion object {
        /**
         * Creates an exception for an error response with [status], carrying the server's details. The reason is
         * [reason] when given, otherwise classified from [status].
         */
        fun requestFailed(
            status: HttpStatusCode,
            reason: Reason? = null,
            cause: Exception? = null,
            serverMessage: String? = null,
            retryAfter: Duration? = null,
            validationIssues: List<ValidationIssue> = emptyList(),
        ): YouVersionNetworkException {
            val resolvedReason =
                reason ?: when (status.value) {
                    HttpStatusCode.TooManyRequests.value -> Reason.RATE_LIMITED
                    in 500..599 -> Reason.SERVER_ERROR
                    in 400..499 -> Reason.REQUEST_REJECTED
                    else -> Reason.REQUEST_FAILED
                }
            val summary = "$resolvedReason (${status.value})"
            return YouVersionNetworkException(
                reason = resolvedReason,
                cause = cause,
                message = serverMessage?.let { "$summary: $it" } ?: summary,
                statusCode = status.value,
                serverMessage = serverMessage,
                retryAfterMillis = retryAfter?.inWholeMilliseconds,
                validationIssues = validationIssues,
            )
        }
    }
}

internal fun notPermitted() = YouVersionNetworkException(YouVersionNetworkException.Reason.NOT_PERMITTED)

internal fun invalidResponse(e: Exception) =
    YouVersionNetworkException(YouVersionNetworkException.Reason.INVALID_RESPONSE, e)
