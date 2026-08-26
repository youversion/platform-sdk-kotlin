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
class BibleVersionMemoryCache(
    private val now: () -> Long = System::currentTimeMillis,
) : BibleVersionCache {
    private val versionCache = mutableMapOf<Int, CachedBibleContent<BibleVersion>>()
    private val versionMutex = Mutex()

    private val chapterCache = mutableMapOf<String, CachedBibleContent<String>>()
    private val chapterMutex = Mutex()

    private val cacheType = "MemoryCache"

    private fun cacheKey(reference: BibleReference): String = "${reference.versionId}_${reference.chapterUSFM}"

    private fun CachedBibleContent<*>.isExpired(): Boolean = expiresAt != null && expiresAt <= now()

    // ----- BibleVersionCache
    override val storedVersionIds: List<Int>
        get() = versionCache.keys.toList()

    override suspend fun version(id: Int): CachedBibleContent<BibleVersion>? =
        versionMutex.withLock {
            val cached = versionCache[id] ?: return@withLock null
            if (cached.isExpired()) {
                versionCache.remove(id)
                null
            } else {
                cached
            }
        }

    override suspend fun chapterContent(reference: BibleReference): CachedBibleContent<String>? =
        chapterMutex.withLock {
            val cacheKey = cacheKey(reference)
            val cached = chapterCache[cacheKey] ?: return@withLock null
            if (cached.isExpired()) {
                chapterCache.remove(cacheKey)
                null
            } else {
                cached
            }
        }

    override suspend fun addVersion(
        version: BibleVersion,
        expiresAt: Long?,
    ) = versionMutex.withLock {
        versionCache[version.id] = CachedBibleContent(version, expiresAt)
    }

    override suspend fun addChapterContents(
        content: String,
        reference: BibleReference,
        expiresAt: Long?,
    ) = chapterMutex.withLock {
        chapterCache[cacheKey(reference)] = CachedBibleContent(content, expiresAt)
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

    override suspend fun removeExpiredEntries() {
        versionMutex.withLock {
            versionCache.entries.removeAll { it.value.isExpired() }
        }
        chapterMutex.withLock {
            chapterCache.entries.removeAll { it.value.isExpired() }
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
