package com.youversion.platform.core.bibles.domain

import android.content.Context
import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.bibles.data.BibleVersionPersistentCache
import com.youversion.platform.core.bibles.data.BibleVersionTemporaryCache
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.utilities.dependencies.SharedPreferencesStore
import com.youversion.platform.core.utilities.dependencies.Store
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

enum class BibleVersionDownloadStatus {
    DOWNLOADABLE,
    DOWNLOADED,
    NOT_DOWNLOADABLE,
}

class BibleVersionRepository(
    private val memoryCache: BibleVersionCache,
    private val temporaryCache: BibleVersionCache,
    private val persistentCache: BibleVersionCache,
    private val store: Store,
) {
    constructor(context: Context) : this(
        memoryCache = BibleVersionMemoryCache(),
        temporaryCache = BibleVersionTemporaryCache(context),
        persistentCache = BibleVersionPersistentCache(context),
        store = SharedPreferencesStore(context),
    )

    private val inFlightTasks = mutableMapOf<Int, Deferred<BibleVersion>>()

    // ----- Versions
    suspend fun versionIfCached(id: Int): BibleVersion? =
        memoryCache.version(id)
            ?: temporaryCache.version(id)
            ?: persistentCache.version(id)

    suspend fun version(id: Int): BibleVersion {
        // Try to get from cache first
        try {
            versionIfCached(id)?.let { return it }
        } catch (e: Exception) {
            println("BibleVersionRepository.version: $e")
        }

        // If a fetch is already in-flight, await its result
        inFlightTasks[id]?.let { task ->
            return task.await()
        }

        // Otherwise, create a new fetch task
        val deferred = CompletableDeferred<BibleVersion>()
        inFlightTasks[id] = deferred

        return try {
            val version = YouVersionApi.bible.version(id)
            temporaryCache.addVersion(version)
            memoryCache.addVersion(version)
            persistentCache.addVersion(version)
            deferred.complete(version)
            version
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
            throw e
        } finally {
            inFlightTasks.remove(id)
        }
    }

    fun versionIsPresent(id: Int): Boolean = persistentCache.versionIsPresent(id)

    val downloadedVersions: List<Int>
        get() = persistentCache.storedVersionIds

    /**
     * Returns the a BibleVersion that the User prefers to use,
     * depending on if one has been downloaded or if one has
     * been saved as a favorite. Otherwise, fallback to NIV.
     */
    val preferredVersionId: Int
        get() =
            downloadedVersions.firstOrNull()
                ?: store.myVersionIds?.firstOrNull()
                ?: 111 // NIV

    suspend fun downloadVersion(id: Int) {
        if (persistentCache.versionIsPresent(id)) {
            return
        }
        val version = version(id)
        persistentCache.addVersion(version)
        temporaryCache.removeVersion(id) // Don't want 2 copies
    }

    fun downloadStatus(id: Int): BibleVersionDownloadStatus {
        if (persistentCache.versionIsPresent(id)) {
            return BibleVersionDownloadStatus.DOWNLOADED
        }

        // TODO: look at the BibleVersion to see if it's downloadable or not.
        return BibleVersionDownloadStatus.NOT_DOWNLOADABLE
    }

    suspend fun removeVersion(id: Int) {
        memoryCache.removeVersion(id)
        temporaryCache.removeVersion(id)
        persistentCache.removeVersion(id)
    }

    suspend fun removeUnpermittedVersions(permittedIds: Set<Int>) {
        memoryCache.removeUnpermittedVersions(permittedIds)
        temporaryCache.removeUnpermittedVersions(permittedIds)
        persistentCache.removeUnpermittedVersions(permittedIds)
    }

    // ----- Chapters
    suspend fun chapter(reference: BibleReference): String {
        val logKey = "${reference.versionId}_${reference.chapterUSFM}"
        memoryCache.chapterContent(reference)?.let {
            return it
        }

        temporaryCache.chapterContent(reference)?.let {
            memoryCache.addChapterContents(it, reference)
            return it
        }

        persistentCache.chapterContent(reference)?.let {
            memoryCache.addChapterContents(it, reference)
            return it
        }

        Logger.d { "$logKey Not found in any cache. Fetching from network..." }
        val contents = YouVersionApi.bible.passage(reference).content
        memoryCache.addChapterContents(contents, reference)
        temporaryCache.addChapterContents(contents, reference)
        persistentCache.addChapterContents(contents, reference)

        return contents
    }

    suspend fun removeVersionChapters(versionId: Int) {
        memoryCache.removeVersionChapters(versionId)
        temporaryCache.removeVersionChapters(versionId)
        persistentCache.removeVersionChapters(versionId)
    }

    // ----- User Bible Preferences
    suspend fun preferredBibleVersion(): BibleVersion = version(preferredVersionId)
}
