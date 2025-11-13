package com.youversion.platform.core.utilities.exceptions

class BibleVersionApiException(
    val reason: Reason,
    cause: Exception? = null,
) : Exception(reason.toString().lowercase(), cause) {
    enum class Reason {
        CANNOT_DOWNLOAD,
        INVALID_DOWNLOAD,
        NOT_PERMITTED,
        INVALID_RESPONSE,
    }
}
