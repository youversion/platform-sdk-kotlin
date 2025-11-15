package com.youversion.platform.core.bibles.data

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion

/**
 * Interfacing for caching Bible Version metadata and chapter contents locally, either
 * in memory or on disk, based on the implementation.
 */
interface BibleVersionCache {
    /**
     * A list of all version IDs currently stored in the cache.
     *
     * ### Example:
     * ```kotlin
     * val versionIds = cache.storedVersionIds
     * // [111, 206, ...]
     * ```
     *
     * @return A list of version IDs.
     */
    val storedVersionIds: List<Int>

    /**
     * Retrieves a [BibleVersion] from the cache.
     *
     * ### Example:
     * ```kotlin
     * val version = cache.version(111)
     * // BibleVersion(id=111, abbreviation="NIV", ...)
     * ```
     *
     * @param id The ID of the version to retrieve.
     * @return The [BibleVersion] if found, or null if not found.
     */
    suspend fun version(id: Int): BibleVersion?

    /**
     * Retrieves chapter content from the cache.
     *
     * ### Example:
     * ```kotlin
     * val content = cache.chapterContent(
     *     BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verseStart = 1)
     * )
     * // "<html><body><p>In the beginning..."
     * ```
     *
     * @param reference The [BibleReference] for the chapter.
     * @return The chapter's content, or null if not found.
     */
    suspend fun chapterContent(reference: BibleReference): String?

    /**
     * Adds a [BibleVersion] to the cache.
     *
     * @param version The [BibleVersion] to add.
     */
    suspend fun addVersion(version: BibleVersion)

    /**
     * Adds chapter content to the cache.
     *
     * @param content The chapter content to add.
     * @param reference The [BibleReference] for the chapter.
     */
    suspend fun addChapterContents(
        content: String,
        reference: BibleReference,
    )

    /**
     * Removes a [BibleVersion]'s metadata and all chapters from the cache.
     *
     * @param versionId The ID of the [BibleVersion] to remove.
     */
    suspend fun removeVersion(versionId: Int)

    /**
     * Removes all chapters for a given Bible version from the cache.
     *
     * @param versionId The ID of the Bible version whose chapters to remove.
     */
    suspend fun removeVersionChapters(versionId: Int)

    /**
     * Removes all [BibleVersion]s from the cache that are not in the provided
     * set of permitted IDs. Passing an empty set will remove all versions.
     *
     * @param permittedIds A set of IDs for [BibleVersion]s that are allowed to remain in the cache.
     */
    suspend fun removeUnpermittedVersions(permittedIds: Set<Int>)

    /**
     * Checks if a [BibleVersion] is present in the cache.
     *
     * ### Example:
     * ```kotlin
     * val isPresent = cache.versionIsPresent(111)
     * // true
     * ```
     *
     * @param versionId The ID of the [BibleVersion] to check.
     * @return True if the [BibleVersion] is present, false otherwise.
     */
    fun versionIsPresent(versionId: Int): Boolean

    /**
     * Checks if any chapters for a Bible version are present in the cache.
     *
     * ### Example:
     * ```kotlin
     * val arePresent = cache.chaptersArePresent(111)
     * // true
     * ```
     *
     * @param versionId The ID of the [BibleVersion] to check.
     * @return True if chapters are present, false otherwise.
     */
    fun chaptersArePresent(versionId: Int): Boolean
}
