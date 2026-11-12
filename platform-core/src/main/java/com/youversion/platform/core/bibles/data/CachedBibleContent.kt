package com.youversion.platform.core.bibles.data

/**
 * A cached value together with the time it expires, as epoch milliseconds.
 * A null [expiresAt] means the value never expires (downloaded content).
 */
public class CachedBibleContent<T>(
    public val value: T,
    public val expiresAt: Long?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CachedBibleContent<*>) return false
        return value == other.value &&
            expiresAt == other.expiresAt
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + expiresAt.hashCode()
        return result
    }

    override fun toString(): String = "CachedBibleContent(value=$value, expiresAt=$expiresAt)"
}
