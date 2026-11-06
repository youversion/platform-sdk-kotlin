package com.youversion.platform.core.utilities.exceptions

public class BibleVersionApiException(
    public val reason: Reason,
    cause: Exception? = null,
) : Exception(reason.toString().lowercase(), cause) {
    public enum class Reason {
        CANNOT_DOWNLOAD,
        INVALID_DOWNLOAD,
        NOT_PERMITTED,
        INVALID_RESPONSE,
    }
}
