package com.youversion.platform.core.domain

/**
 * A simple Key-value storage.
 */
public interface Storage {
    /**
     * Stores a string value for a given key. If the String is null,
     * then the key is removed.
     */
    public fun putString(
        key: String,
        value: String?,
    )

    /**
     * @return The string value for a given key, or null if the key
     * does not exist.
     */
    public fun getStringOrNull(key: String): String?

    /**
     * Stores an integer value for a given key. If the Integer is null,
     * then the key is removed.
     */
    public fun putInt(
        key: String,
        value: Int?,
    )

    /**
     * @return The integer value for a given key, or null if the key
     * does not exist.
     */
    public fun getIntOrNull(key: String): Int?

    /**
     * Stores a float value for a given key. If the Float is null,
     * then the key is removed.
     */
    public fun putFloat(
        key: String,
        value: Float?,
    )

    /**
     * @return The float value for a given key, or null if the key
     * does not exist.
     */
    public fun getFloatOrNull(key: String): Float?
}
