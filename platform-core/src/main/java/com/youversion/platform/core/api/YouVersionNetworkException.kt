package com.youversion.platform.core.api

class YouVersionNetworkException(
    val reason: Reason,
    cause: Exception? = null,
) : Exception(reason.toString().lowercase(), cause) {
    enum class Reason {
        /**
         * The server returns an error response
         */
        CANNOT_DOWNLOAD,

        /**
         * If given a [com.youversion.platform.core.bibles.domain.BibleReference], it did not
         * contain a valid chapter.
         */
        INVALID_DOWNLOAD,

        /**
         * The App Key is invalid or lacks permission
         */
        NOT_PERMITTED,

        /**
         * The response is not valid.
         */
        INVALID_RESPONSE,
    }
}

internal fun notPermitted() = YouVersionNetworkException(YouVersionNetworkException.Reason.NOT_PERMITTED)

internal fun cannotDownload() = YouVersionNetworkException(YouVersionNetworkException.Reason.CANNOT_DOWNLOAD)

internal fun invalidResponse(e: Exception) =
    YouVersionNetworkException(YouVersionNetworkException.Reason.INVALID_RESPONSE, e)
