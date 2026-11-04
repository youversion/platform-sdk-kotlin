package com.youversion.platform.core.api

import io.ktor.http.HttpStatusCode
import kotlin.time.Duration

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
    /**
     * How long the server asks the app to wait before retrying, from the `Retry-After` header.
     */
    public val retryAfter: Duration?,
    /**
     * The request parameters the server found invalid. Empty when it reports none.
     */
    public val validationIssues: List<ValidationIssue>,
) : Exception(message, cause) {
    /**
     * Creates an exception for [reason], with its name as the message.
     */
    public constructor(reason: Reason, cause: Exception? = null) : this(
        reason = reason,
        cause = cause,
        message = reason.toString(),
        statusCode = null,
        serverMessage = null,
        retryAfter = null,
        validationIssues = emptyList(),
    )

    /**
     * Why a request failed. More values may be added in a minor release, so a `when` over [reason]
     * needs an `else` branch.
     */
    public class Reason private constructor(
        private val name: String,
    ) {
        override fun toString(): String = name

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
        /**
         * The path to the invalid value, such as `["query", "language_ranges"]`.
         */
        public val location: List<String>,
        /**
         * Why the value is invalid.
         */
        public val message: String,
    ) {
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
            serverMessage: String? = null,
            retryAfter: Duration? = null,
            validationIssues: List<ValidationIssue> = emptyList(),
        ): YouVersionNetworkException {
            val reason =
                reason ?: when (status.value) {
                    HttpStatusCode.TooManyRequests.value -> Reason.RATE_LIMITED
                    in 500..599 -> Reason.SERVER_ERROR
                    in 400..499 -> Reason.REQUEST_REJECTED
                    else -> Reason.REQUEST_FAILED
                }
            val summary = "$reason (${status.value})"
            return YouVersionNetworkException(
                reason = reason,
                cause = null,
                message = serverMessage?.let { "$summary: $it" } ?: summary,
                statusCode = status.value,
                serverMessage = serverMessage,
                retryAfter = retryAfter,
                validationIssues = validationIssues,
            )
        }
    }
}

internal fun notPermitted() = YouVersionNetworkException(YouVersionNetworkException.Reason.NOT_PERMITTED)

internal fun invalidResponse(e: Exception) =
    YouVersionNetworkException(YouVersionNetworkException.Reason.INVALID_RESPONSE, e)
