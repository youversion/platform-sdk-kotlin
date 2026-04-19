package com.youversion.platform.core.bibles.domain

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Collator

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
    private val inFlightTasksMutex = Mutex()

    /** In-memory cache of bible versions which have been fetched by language */
    private var versionsInLanguage: MutableMap<String, List<BibleVersion>> = mutableMapOf()

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
        inFlightTasksMutex
            .withLock { inFlightTasks[id] }
            ?.let { task ->
                if (task.isActive) return task.await()
            }

        // Otherwise, create a new fetch task
        val deferred = CompletableDeferred<BibleVersion>()
        inFlightTasksMutex.withLock { inFlightTasks[id] = deferred }

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
            inFlightTasksMutex.withLock { inFlightTasks.remove(id) }
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

    /** Holds minimal information about all Bible versions available to this app, in all languages. */
    var permittedVersions: List<BibleVersion>? = null
        private set

    /**
     * Returns minimal information about all Bible versions available to this app, in all languages
     */
    suspend fun permittedVersionsListing(): List<BibleVersion> =
        permittedVersions
            ?: permittedVersions()
                .also { permittedVersions = it }

    suspend fun fullVersions(languageTag: String): List<BibleVersion> {
        versionsInLanguage[languageTag]?.let {
            return it
        }

        // There is currently no language with more than 99 versions so ignore pagination for now
        val unsortedVersions =
            YouVersionApi.bible
                .versions(languageCode = languageTag, pageSize = 99)
                .data

        fun comparableString(bibleVersion: BibleVersion): String =
            bibleVersion.localizedTitle ?: bibleVersion.title ?: bibleVersion.localizedAbbreviation
                ?: bibleVersion.abbreviation
                ?: bibleVersion.id.toString()

        // collator allows for locale-specific string comparisons
        val collator = Collator.getInstance()
        val result =
            unsortedVersions
                .distinctBy { it.id }
                .sortedWith { a, b ->
                    val aTitle = comparableString(a).lowercase()
                    val bTitle = comparableString(b).lowercase()
                    collator.compare(aTitle, bTitle)
                }
        versionsInLanguage[languageTag] = result
        return result
    }
}
