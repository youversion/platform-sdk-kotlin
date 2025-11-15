package com.youversion.platform.core.bibles.data

import android.content.Context
import co.touchlab.kermit.Logger
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A file-based implementation of [BibleVersionCache] that stores data in the app's
 * cache directory.
 */
class BibleVersionTemporaryCache(
    private val context: Context,
) : BibleVersionFileCache() {
    override val rootDir: File
        get() = context.cacheDir

    override val cacheType: String = "TemporaryCache"
}

/**
 * A file-based implementation of [BibleVersionCache] that stores data in the app's
 * files directory.
 */
class BibleVersionPersistentCache(
    private val context: Context,
) : BibleVersionFileCache() {
    override val rootDir: File
        get() = context.filesDir

    override val cacheType: String = "PersistentCache"
}

/**
 * A file-based implementation of [BibleVersionCache]. Data is stored in a directory
 * on the device. An example directory structure is shown below:
 *
 * ```
 * rootDir/
 *   └── bible_206/
 *       └── metadata.json
 *       └── chapters
 *          └── GEN.1
 *          └── GEN.2
 *   └── bible_111/
 *       └── metadata.json
 *       └── chapters/
 *          └── GEN.1
 *          └── GEN.2
 * ```
 */
abstract class BibleVersionFileCache : BibleVersionCache {
    // Use a mutex to ensure thread safety when accessing the cache. A more
    // advanced implementation would be to create a mutex per file, but
    // contention is not expected to be high enough to justify the overhead.
    private val mutex = Mutex()

    // the rootDir/
    protected abstract val rootDir: File

    // The type of files dir used for  the rootDir/ e.g. cacheDir or filesDir
    protected abstract val cacheType: String

    /** @return [File] representing bible_<id>/ dir */
    private fun bibleVersionDir(id: Int): File = ensureExists(File(rootDir, "bible_$id"))

    /** @return [File] representing bible_<id>/metadata.json file */
    private fun bibleVersionMetadataFile(id: Int): File = File(bibleVersionDir(id), "metadata.json")

    /** @return [File] representing bible_<id>/chapters/ dir */
    private fun bibleVersionChaptersDir(id: Int): File = ensureExists(File(bibleVersionDir(id), "chapters"))

    /** @return [File] representing bible_<id>/chapters/<usfm> file */
    private fun chapterContentsFile(
        usfm: String,
        versionId: Int,
    ): File = File(bibleVersionChaptersDir(versionId), usfm)

    // ----- BibleVersionCache
    override val storedVersionIds: List<Int>
        get() = scanForVersionIds(rootDir)

    override suspend fun version(id: Int): BibleVersion? =
        withContext(Dispatchers.IO) {
            Logger.d { "[$cacheType] Checking for version id=$id" }
            if (!versionIsPresent(id)) {
                Logger.d { "[$cacheType] Version id=$id not found" }
                return@withContext null
            }
            mutex.withLock {
                val metadataJson = bibleVersionMetadataFile(id).readText()
                val version = Json.decodeFromString<BibleVersion>(metadataJson)
                Logger.d { "[$cacheType] Version id=$id found: ${version.abbreviation}" }
                version
            }
        }

    override suspend fun chapterContent(reference: BibleReference): String? =
        withContext(Dispatchers.IO) {
            val usfm = reference.chapterUSFM
            val file = chapterContentsFile(usfm, reference.versionId)

            Logger.d { "[$cacheType] Checking for chapter $usfm in version ${reference.versionId}" }
            if (!file.exists()) {
                Logger.d { "[$cacheType] Chapter $usfm not found in version ${reference.versionId}" }
                return@withContext null
            }
            mutex.withLock {
                val contents = file.readText()
                Logger.d {
                    "[$cacheType] Chapter $usfm found in version ${reference.versionId}: ${contents.length} characters"
                }
                contents
            }
        }

    override suspend fun addVersion(version: BibleVersion) =
        withContext(Dispatchers.IO) {
            Logger.d { "[$cacheType] Adding version id=${version.id} abbreviation=${version.abbreviation}" }
            mutex.withLock {
                bibleVersionMetadataFile(version.id)
                    .writeText(Json.encodeToString(version))
            }
            Logger.d { "[$cacheType] Successfully added version id=${version.id}" }
        }

    override suspend fun addChapterContents(
        content: String,
        reference: BibleReference,
    ) = withContext(Dispatchers.IO) {
        val usfm = reference.chapterUSFM

        Logger.d { "[$cacheType] Adding chapter content for $usfm in version ${reference.versionId}" }
        mutex.withLock {
            chapterContentsFile(usfm, reference.versionId)
                .writeText(content)
        }
        Logger.d { "[$cacheType] Successfully added chapter content for $usfm in version ${reference.versionId}" }
    }

    override suspend fun removeVersion(versionId: Int) {
        withContext(Dispatchers.IO) {
            Logger.d { "[$cacheType] Removing version id=$versionId" }
            val deleted = mutex.withLock { bibleVersionMetadataFile(versionId).delete() }
            Logger.d { "[$cacheType] Version id=$versionId removed: $deleted" }
        }
    }

    override suspend fun removeVersionChapters(versionId: Int) {
        withContext(Dispatchers.IO) {
            Logger.d { "[$cacheType] Removing chapters for version id=$versionId" }
            val deleted = mutex.withLock { bibleVersionChaptersDir(versionId).deleteRecursively() }
            Logger.d { "[$cacheType] Chapters for version id=$versionId removed: $deleted" }
        }
    }

    override suspend fun removeUnpermittedVersions(permittedIds: Set<Int>) {
        withContext(Dispatchers.IO) {
            Logger.d { "[$cacheType] Scanning for unpermitted versions, permitted count=${permittedIds.size}" }
            val unpermittedIds = storedVersionIds.filterNot { permittedIds.contains(it) }
            Logger.d { "[$cacheType] Found ${unpermittedIds.size} unpermitted versions to remove: $unpermittedIds" }
            unpermittedIds.forEach { removeVersion(it) }
            Logger.d { "[$cacheType] Finished removing unpermitted versions" }
        }
    }

    override fun versionIsPresent(versionId: Int): Boolean {
        val exists = bibleVersionMetadataFile(versionId).exists()
        Logger.d { "[$cacheType] Version id=$versionId present: $exists" }
        return exists
    }

    override fun chaptersArePresent(versionId: Int): Boolean {
        val hasChapters = bibleVersionChaptersDir(versionId).listFiles()?.isNotEmpty() == true
        Logger.d { "[$cacheType] Version id=$versionId chapters are present: $hasChapters" }
        return hasChapters
    }

    // ----- Private Helpers
    private fun ensureExists(file: File): File {
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }

    private fun scanForVersionIds(dir: File): List<Int> {
        val prefix = "bible_"

        return dir
            .listFiles()
            ?.filter { it.isDirectory && !it.isHidden }
            ?.mapNotNull { file ->
                val name = file.name
                if (name.startsWith(prefix)) {
                    val suffix = name.removePrefix(prefix)
                    if (suffix.all { it.isDigit() } && suffix.length < 7) {
                        suffix.toIntOrNull()
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            ?: emptyList()
    }
}
