package com.youversion.platform.core.domain

/**
 * A simple Key-value storage.
 */
interface Storage {
    /**
     * Stores a string value for a given key. If the String is null,
     * then the key is removed.
     */
    fun putString(
        key: String,
        value: String?,
    )

    /**
     * @return The string value for a given key, or null if the key
     * does not exist.
     */
    fun getStringOrNull(key: String): String?

    /**
     * Stores an integer value for a given key. If the Integer is null,
     * then the key is removed.
     */
    fun putInt(
        key: String,
        value: Int?,
    )

    /**
     * @return The integer value for a given key, or null if the key
     * does not exist.
     */
    fun getIntOrNull(key: String): Int?

    /**
     * Stores a float value for a given key. If the Float is null,
     * then the key is removed.
     */
    fun putFloat(
        key: String,
        value: Float?,
    )

    /**
     * @return The float value for a given key, or null if the key
     * does not exist.
     */
    fun getFloatOrNull(key: String): Float?

    /**
     * Stores a long value for a given key. If the Long is null,
     * then the key is removed.
     */
    fun putLong(
        key: String,
        value: Long?,
    )

    /**
     * @return The long value for a given key, or null if the key
     * does not exist.
     */
    fun getLongOrNull(key: String): Long?
}
