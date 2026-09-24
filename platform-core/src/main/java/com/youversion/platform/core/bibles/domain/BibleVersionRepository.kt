package com.youversion.platform.core.bibles.domain

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.api.BiblesEndpoints
import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.core.utilities.InFlightTasks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Collator
import java.util.Locale

internal enum class BibleVersionDownloadStatus {
    DOWNLOADABLE,
    DOWNLOADED,
    NOT_DOWNLOADABLE,
}

class BibleVersionRepository(
    private val memoryCache: BibleVersionCache,
    private val temporaryCache: BibleVersionCache,
    private val persistentCache: BibleVersionCache,
) {
    private val inFlightTasks = InFlightTasks<Int, BibleVersion>()

    /** In-memory cache of bible versions which have been fetched by language */
    private var versionsInLanguage: MutableMap<String, List<BibleVersion>> = mutableMapOf()
    private val fullVersionsMutex = Mutex()

    // ----- Versions
    suspend fun versionIfCached(id: Int): BibleVersion? {
        memoryCache.version(id)?.let { return it.value }

        temporaryCache.version(id)?.let {
            memoryCache.addVersion(it.value, it.expiresAt)
            return it.value
        }

        persistentCache.version(id)?.let {
            memoryCache.addVersion(it.value)
            return it.value
        }

        return null
    }

    suspend fun version(id: Int): BibleVersion {
        // Try to get from cache first
        try {
            versionIfCached(id)?.let { return it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("BibleVersionRepository.version: $e")
        }

        return inFlightTasks.result(id) {
            val response = BiblesEndpoints.versionResponse(id)
            val version = response.value
            memoryCache.addVersion(version, response.expiresAt)
            if (response.isCacheable) {
                temporaryCache.addVersion(version, response.expiresAt)
            }
            version
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

    internal fun downloadStatus(id: Int): BibleVersionDownloadStatus {
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
        removeExpiredContent()
        val allowedIds = permittedIds - YouVersionPlatformConfiguration.excludedVersionIds
        memoryCache.removeUnpermittedVersions(allowedIds)
        temporaryCache.removeUnpermittedVersions(allowedIds)
        persistentCache.removeUnpermittedVersions(allowedIds)
    }

    /** Removes expired entries from every cache tier. */
    suspend fun removeExpiredContent() {
        memoryCache.removeExpiredEntries()
        temporaryCache.removeExpiredEntries()
        persistentCache.removeExpiredEntries()
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
            .filter { it.isPermittedByConfiguration() }

    /** Holds minimal information about all Bible versions available to this app, in all languages. */
    var permittedVersions: List<BibleVersion>? = null
        private set
    private val permittedVersionsMutex = Mutex()

    /**
     * Returns minimal information about all Bible versions available to this app, in all languages
     */
    suspend fun permittedVersionsListing(): List<BibleVersion> =
        permittedVersionsMutex.withLock {
            permittedVersions ?: permittedVersions().also { permittedVersions = it }
        }

    /**
     * Clears the in-memory listings of versions returned by [permittedVersionsListing] and [fullVersions].
     * Call this when the configured [YouVersionPlatformConfiguration.permittedLanguageTags],
     * [YouVersionPlatformConfiguration.permittedVersionIds] or
     * [YouVersionPlatformConfiguration.excludedVersionIds] change so callers do not receive stale
     * filtered results from a previous configuration.
     *
     * Reassigns rather than mutates the language-keyed map so a fetch already running under
     * [fullVersionsMutex] keeps its own reference and only its post-fetch write becomes orphaned —
     * the next call reads the fresh map and triggers a refetch.
     */
    fun clearVersionListings() {
        permittedVersions = null
        versionsInLanguage = mutableMapOf()
    }

    suspend fun fullVersions(languageTag: String): List<BibleVersion> =
        fullVersionsMutex.withLock {
            versionsInLanguage[languageTag]?.let { return@withLock it }

            // There is currently no language with more than 99 versions so ignore pagination for now
            val unsortedVersions =
                YouVersionApi.bible
                    .versions(languageCode = languageTag, pageSize = 99)
                    .data
                    .filter { it.isPermittedByConfiguration() }

            fun comparableString(bibleVersion: BibleVersion): String =
                bibleVersion.localizedTitle ?: bibleVersion.title ?: bibleVersion.localizedAbbreviation
                    ?: bibleVersion.abbreviation
                    ?: bibleVersion.id.toString()

            // collator allows for locale-specific string comparisons
            val collator = Collator.getInstance(Locale.forLanguageTag(languageTag))
            val result =
                unsortedVersions
                    .distinctBy { it.id }
                    .sortedWith { a, b ->
                        val aTitle = comparableString(a).lowercase()
                        val bTitle = comparableString(b).lowercase()
                        collator.compare(aTitle, bTitle)
                    }
            versionsInLanguage[languageTag] = result
            result
        }
}

/**
 * Returns true when this version satisfies the configured
 * [YouVersionPlatformConfiguration.excludedVersionIds],
 * [YouVersionPlatformConfiguration.permittedLanguageTags] and
 * [YouVersionPlatformConfiguration.permittedVersionIds] filters. A `null` allowlist means
 * "no restriction" on that dimension; an exclusion always wins over
 * [YouVersionPlatformConfiguration.permittedVersionIds].
 */
@PlatformInternalApi
fun BibleVersion.isPermittedByConfiguration(): Boolean {
    if (!YouVersionPlatformConfiguration.isPermittedByVersionIdFilters(id)) return false
    YouVersionPlatformConfiguration.permittedLanguageTags?.let { permittedTags ->
        if (languageTag == null || languageTag !in permittedTags) return false
    }
    return true
}
