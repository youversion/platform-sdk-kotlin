package com.youversion.platform.core.bibles.domain

import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.models.BibleVersion
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
) {
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

    suspend fun permittedVersions(languageTag: String? = null): List<BibleVersion> =
        YouVersionApi.bible
            .versions(
                languageCode = languageTag,
                fields =
                    listOf(
                        BibleVersion.CodingKey.ID,
                        BibleVersion.CodingKey.LANGUAGE_TAG,
                    ),
            ).data

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
}
