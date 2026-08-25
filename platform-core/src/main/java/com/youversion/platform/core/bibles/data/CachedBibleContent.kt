package com.youversion.platform.core.bibles.data

/**
 * A cached value together with the time it expires, as epoch milliseconds.
 * A null [expiresAt] means the value never expires (downloaded content).
 */
data class CachedBibleContent<T>(
    val value: T,
    val expiresAt: Long?,
)
