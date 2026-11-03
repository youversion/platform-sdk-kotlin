package com.youversion.platform.core.api

import io.ktor.http.HttpStatusCode

class YouVersionNetworkException internal constructor(
    val reason: Reason,
    cause: Exception?,
    /**
     * Why the server rejected the request, when [reason] is [Reason.CANNOT_DOWNLOAD] and the status
     * is one the SDK classifies. `null` for every other [reason] and for unclassified statuses.
     */
    val failure: Failure?,
) : Exception(reason.toString().lowercase(), cause) {
    constructor(reason: Reason, cause: Exception? = null) : this(reason, cause, null)

    enum class Reason {
        /**
         * The server returns an error response. Check [failure] for whether it is temporary.
         */
        CANNOT_DOWNLOAD,

        /**
         * If given a [com.youversion.platform.core.bibles.domain.BibleReference], it did not
         * contain a valid chapter.
         */
        INVALID_DOWNLOAD,

        /**
         * The App Key is invalid or lacks permission, or the signed-in user has not granted the
         * permission the request requires. Retrying does not change either outcome.
         */
        NOT_PERMITTED,

        /**
         * The response is not valid.
         */
        INVALID_RESPONSE,

        /**
         *  Missing authentication
         */
        MISSING_AUTHENTICATION,
    }

    // TODO(next major): replace with Reason values RATE_LIMITED, SERVER_ERROR and REQUEST_REJECTED.
    enum class Failure {
        /**
         * The server returned `429 Too Many Requests`. The request may succeed if retried later.
         */
        RATE_LIMITED,

        /**
         * The server returned a `5xx` status. The request may succeed if retried later.
         */
        SERVER_ERROR,

        /**
         * The server rejected the request with a `4xx` status, such as for a resource the app cannot
         * access. Retrying the same request does not help. `429` is [RATE_LIMITED], and statuses an
         * endpoint reports as [Reason.NOT_PERMITTED] carry no failure.
         */
        REQUEST_REJECTED,
    }
}

internal fun notPermitted() = YouVersionNetworkException(YouVersionNetworkException.Reason.NOT_PERMITTED)

internal fun cannotDownload(status: HttpStatusCode) =
    YouVersionNetworkException(
        reason = YouVersionNetworkException.Reason.CANNOT_DOWNLOAD,
        cause = null,
        failure =
            when (status.value) {
                HttpStatusCode.TooManyRequests.value -> YouVersionNetworkException.Failure.RATE_LIMITED
                in 500..599 -> YouVersionNetworkException.Failure.SERVER_ERROR
                in 400..499 -> YouVersionNetworkException.Failure.REQUEST_REJECTED
                else -> null
            },
    )

internal fun invalidResponse(e: Exception) =
    YouVersionNetworkException(YouVersionNetworkException.Reason.INVALID_RESPONSE, e)
