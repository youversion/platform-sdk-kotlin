package com.youversion.platform.core.bibles.data

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A memory-based implementation of [BibleVersionCache]. Since there is no file system
 * with a directory structure, all cached data is stored in a two maps with the
 * following structure:
 *
 * ```
 * // Version Metadata Map
 * {
 *   206: <BibleVersion>, // Bible 206 metadata
 *   111: <BibleVersion>, // Bible 111 metadata
 * }
 *
 * // Chapter Contents Map
 * {
 *   "206_GEN.1": "<string>", // Chapter contents for Gen.1 in Bible 206
 *   "206_GEN.2": "<string>", // Chapter contents for Gen.2 in Bible 206
 *   "111_GEN.1": "<string>", // Chapter contents for Gen.1 in Bible 111
 *   "111_GEN.2": "<string>", // Chapter contents for Gen.2 in Bible 111
 * }
 * ```
 *
 * Cache is volatile and will be lost when the app is closed.
 */
class BibleVersionMemoryCache : BibleVersionCache {
    private val versionCache = mutableMapOf<Int, BibleVersion>()
    private val versionMutex = Mutex()

    private val chapterCache = mutableMapOf<String, String>()
    private val chapterMutex = Mutex()

    private val cacheType = "MemoryCache"

    private fun cacheKey(reference: BibleReference): String = "${reference.versionId}_${reference.chapterUSFM}"

    // ----- BibleVersionCache
    override val storedVersionIds: List<Int>
        get() = versionCache.keys.toList()

    override suspend fun version(id: Int): BibleVersion? =
        versionMutex.withLock {
            val version = versionCache[id]
            version
        }

    override suspend fun chapterContent(reference: BibleReference): String? =
        chapterMutex.withLock {
            val cacheKey = cacheKey(reference)
            val contents = chapterCache[cacheKey]

            contents
        }

    override suspend fun addVersion(version: BibleVersion) =
        versionMutex.withLock {
            versionCache[version.id] = version
        }

    override suspend fun addChapterContents(
        content: String,
        reference: BibleReference,
    ) = chapterMutex.withLock {
        val cacheKey = cacheKey(reference)
        chapterCache[cacheKey] = content
    }

    override suspend fun removeVersion(versionId: Int) {
        versionMutex.withLock {
            versionCache.remove(versionId)
        }
    }

    override suspend fun removeVersionChapters(versionId: Int) {
        chapterMutex.withLock {
            val prefix = "${versionId}_"
            val keysToRemove = chapterCache.keys.filter { it.startsWith(prefix) }
            keysToRemove.forEach { chapterCache.remove(it) }
        }
    }

    override suspend fun removeUnpermittedVersions(permittedIds: Set<Int>) {
        versionMutex.withLock {
            versionCache.entries.removeAll { !permittedIds.contains(it.key) }
        }
    }

    override fun versionIsPresent(versionId: Int): Boolean {
        val present = versionCache.containsKey(versionId)
        return present
    }

    override fun chaptersArePresent(versionId: Int): Boolean {
        val present = chapterCache.keys.any { it.startsWith("${versionId}_") }
        return present
    }
}
