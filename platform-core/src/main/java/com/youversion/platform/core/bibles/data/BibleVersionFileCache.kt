package com.youversion.platform.core.bibles.data

import android.content.Context
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
            if (!versionIsPresent(id)) {
                return@withContext null
            }
            mutex.withLock {
                val metadataJson = bibleVersionMetadataFile(id).readText()
                val version = Json.decodeFromString<BibleVersion>(metadataJson)
                version
            }
        }

    override suspend fun chapterContent(reference: BibleReference): String? =
        withContext(Dispatchers.IO) {
            val usfm = reference.chapterUSFM
            val file = chapterContentsFile(usfm, reference.versionId)

            if (!file.exists()) {
                return@withContext null
            }
            mutex.withLock {
                val contents = file.readText()
                contents
            }
        }

    override suspend fun addVersion(version: BibleVersion) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                bibleVersionMetadataFile(version.id)
                    .writeText(Json.encodeToString(version))
            }
        }

    override suspend fun addChapterContents(
        content: String,
        reference: BibleReference,
    ) = withContext(Dispatchers.IO) {
        val usfm = reference.chapterUSFM

        mutex.withLock {
            chapterContentsFile(usfm, reference.versionId)
                .writeText(content)
        }
    }

    override suspend fun removeVersion(versionId: Int) {
        withContext(Dispatchers.IO) {
            mutex.withLock { bibleVersionMetadataFile(versionId).delete() }
        }
    }

    override suspend fun removeVersionChapters(versionId: Int) {
        withContext(Dispatchers.IO) {
            mutex.withLock { bibleVersionChaptersDir(versionId).deleteRecursively() }
        }
    }

    override suspend fun removeUnpermittedVersions(permittedIds: Set<Int>) {
        withContext(Dispatchers.IO) {
            val unpermittedIds = storedVersionIds.filterNot { permittedIds.contains(it) }
            unpermittedIds.forEach { removeVersion(it) }
        }
    }

    override fun versionIsPresent(versionId: Int): Boolean {
        val exists = bibleVersionMetadataFile(versionId).exists()
        return exists
    }

    override fun chaptersArePresent(versionId: Int): Boolean {
        val hasChapters = bibleVersionChaptersDir(versionId).listFiles()?.isNotEmpty() == true
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
