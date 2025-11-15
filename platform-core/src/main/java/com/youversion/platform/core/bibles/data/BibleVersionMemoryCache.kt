package com.youversion.platform.core.bibles.data

import co.touchlab.kermit.Logger
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
            Logger.d { "[$cacheType] Checking for version id=$id" }
            val version = versionCache[id]
            if (version != null) {
                Logger.d { "[$cacheType] Version id=$id found: ${version.abbreviation}" }
            } else {
                Logger.d { "[$cacheType] Version id=$id not found" }
            }
            version
        }

    override suspend fun chapterContent(reference: BibleReference): String? =
        chapterMutex.withLock {
            val cacheKey = cacheKey(reference)
            Logger.d { "[$cacheType] Checking for $cacheKey chapter contents" }
            val contents = chapterCache[cacheKey]
            if (contents != null) {
                Logger.d { "[$cacheType] Chapter $cacheKey found: ${contents.length} characters" }
            } else {
                Logger.d { "[$cacheType] Chapter $cacheKey not found" }
            }

            contents
        }

    override suspend fun addVersion(version: BibleVersion) =
        versionMutex.withLock {
            Logger.d { "[$cacheType] Adding version id=${version.id} abbreviation=${version.abbreviation}" }
            versionCache[version.id] = version
            Logger.d { "[$cacheType] Successfully added version id=${version.id}" }
        }

    override suspend fun addChapterContents(
        content: String,
        reference: BibleReference,
    ) = chapterMutex.withLock {
        val cacheKey = cacheKey(reference)
        Logger.d { "[$cacheType] Adding chapter content for $cacheKey" }
        chapterCache[cacheKey] = content
        Logger.d { "[$cacheType] Successfully added chapter content for $cacheKey" }
    }

    override suspend fun removeVersion(versionId: Int) =
        versionMutex.withLock {
            Logger.d { "[$cacheType] Removing version id=$versionId" }
            val removed = versionCache.remove(versionId)
            Logger.d { "[$cacheType] Version id=$versionId removed: ${removed != null}" }
        }

    override suspend fun removeVersionChapters(versionId: Int) =
        chapterMutex.withLock {
            Logger.d { "[$cacheType] Removing chapters for version id=$versionId" }
            val prefix = "${versionId}_"
            val keysToRemove = chapterCache.keys.filter { it.startsWith(prefix) }
            keysToRemove.forEach { chapterCache.remove(it) }
            Logger.d { "[$cacheType] ${keysToRemove.size} chapters removed for version id=$versionId" }
        }

    override suspend fun removeUnpermittedVersions(permittedIds: Set<Int>) =
        versionMutex.withLock {
            Logger.d { "[$cacheType] Scanning for unpermitted versions, permitted count=${permittedIds.size}" }
            val unpermittedIds = versionCache.keys.filterNot { permittedIds.contains(it) }.toSet()
            Logger.d { "[$cacheType] Found ${unpermittedIds.size} unpermitted versions to remove: $unpermittedIds" }
            versionCache.entries.removeAll { !permittedIds.contains(it.key) }
            Logger.d { "[$cacheType] Finished removing unpermitted versions" }
        }

    override fun versionIsPresent(versionId: Int): Boolean {
        val present = versionCache.containsKey(versionId)
        Logger.d { "[$cacheType] Version id=$versionId present: $present" }
        return present
    }

    override fun chaptersArePresent(versionId: Int): Boolean {
        val present = chapterCache.keys.any { it.startsWith("${versionId}_") }
        Logger.d { "[$cacheType] Version id=$versionId chapters are present: $present" }
        return present
    }
}
