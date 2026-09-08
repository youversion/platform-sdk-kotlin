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
 * cache directory. Entries expire per the server's caching headers.
 */
internal class BibleVersionTemporaryCache(
    private val context: Context,
    now: () -> Long = System::currentTimeMillis,
) : BibleVersionFileCache(isExpiring = true, now = now) {
    override val rootDir: File
        get() = context.cacheDir
}

/**
 * A file-based implementation of [BibleVersionCache] that holds downloaded versions,
 * which never expire. Downloads live in a dedicated directory under the app's files
 * directory; earlier releases cached all fetched content at the files directory root,
 * and [removeExpiredEntries] removes that previously cached content.
 */
internal class BibleVersionPersistentCache(
    private val context: Context,
) : BibleVersionFileCache(isExpiring = false) {
    override val rootDir: File
        get() = File(context.filesDir, DOWNLOADS_DIR_NAME)

    override suspend fun removeExpiredEntries() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                scanForVersionIds(context.filesDir).forEach {
                    File(context.filesDir, "bible_$it").deleteRecursively()
                }
            }
        }
    }
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
 *
 * When [isExpiring] is true, each cached file gets a `<name>.expiration` sidecar
 * holding its expiration as epoch milliseconds; entries whose sidecar is missing,
 * unreadable, or past are evicted on read and by [removeExpiredEntries].
 */
internal abstract class BibleVersionFileCache(
    private val isExpiring: Boolean,
    private val now: () -> Long = System::currentTimeMillis,
) : BibleVersionCache {
    // Use a mutex to ensure thread safety when accessing the cache. A more
    // advanced implementation would be to create a mutex per file, but
    // contention is not expected to be high enough to justify the overhead.
    protected val mutex = Mutex()

    // the rootDir/
    protected abstract val rootDir: File

    /** @return [File] representing bible_<id>/ dir */
    private fun bibleVersionDir(id: Int): File = File(rootDir, "bible_$id")

    /** @return [File] representing bible_<id>/metadata.json file */
    private fun bibleVersionMetadataFile(id: Int): File = File(bibleVersionDir(id), "metadata.json")

    /** @return [File] representing bible_<id>/chapters/ dir */
    private fun bibleVersionChaptersDir(id: Int): File = File(bibleVersionDir(id), "chapters")

    /** @return [File] representing bible_<id>/chapters/<usfm> file */
    private fun chapterContentsFile(
        usfm: String,
        versionId: Int,
    ): File = File(bibleVersionChaptersDir(versionId), usfm)

    // ----- BibleVersionCache
    override val storedVersionIds: List<Int>
        get() = scanForVersionIds(rootDir)

    override suspend fun version(id: Int): CachedBibleContent<BibleVersion>? =
        withContext(Dispatchers.IO) {
            if (!versionIsPresent(id)) {
                return@withContext null
            }
            mutex.withLock {
                readCached(bibleVersionMetadataFile(id)) { file ->
                    Json.decodeFromString<BibleVersion>(file.readText())
                }
            }
        }

    override suspend fun chapterContent(reference: BibleReference): CachedBibleContent<String>? =
        withContext(Dispatchers.IO) {
            val usfm = reference.chapterUSFM
            val file = chapterContentsFile(usfm, reference.versionId)

            if (!file.exists()) {
                return@withContext null
            }
            mutex.withLock {
                readCached(file) { it.readText() }
            }
        }

    override suspend fun addVersion(
        version: BibleVersion,
        expiresAt: Long?,
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureExists(bibleVersionDir(version.id))
            val file = bibleVersionMetadataFile(version.id)
            file.writeText(Json.encodeToString(version))
            writeExpiration(file, expiresAt)
        }
    }

    override suspend fun addChapterContents(
        content: String,
        reference: BibleReference,
        expiresAt: Long?,
    ) = withContext(Dispatchers.IO) {
        val usfm = reference.chapterUSFM

        mutex.withLock {
            ensureExists(bibleVersionChaptersDir(reference.versionId))
            val file = chapterContentsFile(usfm, reference.versionId)
            file.writeText(content)
            writeExpiration(file, expiresAt)
        }
    }

    override suspend fun removeVersion(versionId: Int) {
        withContext(Dispatchers.IO) {
            mutex.withLock { evict(bibleVersionMetadataFile(versionId)) }
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

    override suspend fun removeExpiredEntries() {
        if (!isExpiring) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                storedVersionIds.forEach { id ->
                    val metadataFile = bibleVersionMetadataFile(id)
                    if (metadataFile.exists() && expirationIsPast(metadataFile)) evict(metadataFile)
                    bibleVersionChaptersDir(id)
                        .listFiles()
                        ?.filterNot { it.name.endsWith(EXPIRATION_SUFFIX) }
                        ?.forEach { if (expirationIsPast(it)) evict(it) }
                }
            }
        }
    }

    override fun versionIsPresent(versionId: Int): Boolean {
        val exists = bibleVersionMetadataFile(versionId).exists()
        return exists
    }

    override fun chaptersArePresent(versionId: Int): Boolean {
        val hasChapters =
            bibleVersionChaptersDir(versionId)
                .listFiles()
                ?.any { !it.name.endsWith(EXPIRATION_SUFFIX) } == true
        return hasChapters
    }

    // ----- Private Helpers
    private fun expirationFile(file: File): File = File(file.parentFile, file.name + EXPIRATION_SUFFIX)

    private fun expirationMillis(file: File): Long? =
        expirationFile(file)
            .takeIf { it.exists() }
            ?.readText()
            ?.trim()
            ?.toLongOrNull()

    private fun expirationIsPast(file: File): Boolean {
        val expiresAt = expirationMillis(file)
        return expiresAt == null || expiresAt <= now()
    }

    private fun evict(file: File) {
        file.delete()
        expirationFile(file).delete()
    }

    private fun <T> readCached(
        file: File,
        read: (File) -> T,
    ): CachedBibleContent<T>? {
        if (!isExpiring) return CachedBibleContent(read(file), null)
        val expiresAt = expirationMillis(file)
        if (expiresAt == null || expiresAt <= now()) {
            evict(file)
            return null
        }
        return CachedBibleContent(read(file), expiresAt)
    }

    private fun writeExpiration(
        file: File,
        expiresAt: Long?,
    ) {
        if (!isExpiring) return
        val sidecar = expirationFile(file)
        if (expiresAt != null) sidecar.writeText(expiresAt.toString()) else sidecar.delete()
    }

    private fun ensureExists(file: File): File {
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    protected fun scanForVersionIds(dir: File): List<Int> {
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

private const val EXPIRATION_SUFFIX = ".expiration"

private const val DOWNLOADS_DIR_NAME = "bible_downloads"
