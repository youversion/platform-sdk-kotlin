package com.youversion.platform.reader.domain

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.domain.Storage
import kotlinx.serialization.json.Json

/**
 * Responsible for fetching and managing data related to the Bible
 * Reader. Note that versions are used by the Bible Reader, but those
 * are managed by the BibleVersionRepository.
 */
class BibleReaderRepository(
    private val json: Json,
    private val storage: Storage,
    private val bibleVersionRepository: BibleVersionRepository,
) {
    companion object {
        private const val NIV_VERSION_ID = 111
        private const val KEY_BIBLE_READER_REFERENCE = "bible-reader-view--reference"
    }

    /**
     * Returns the last Bible reference that the Reader was viewing.
     */
    var lastBibleReference: BibleReference?
        get() =
            storage
                .getStringOrNull(KEY_BIBLE_READER_REFERENCE)
                ?.let { Json.decodeFromString(it) }
        set(value) =
            storage
                .putString(KEY_BIBLE_READER_REFERENCE, value?.let { Json.encodeToString(it) })

    /**
     * Always produces a valid BibleReference based on what is available.
     */
    fun produceBibleReference(bibleReference: BibleReference?): BibleReference =
        bibleReference // Always use the provided reference if available
            ?: lastBibleReference // If no provided reference, use the last saved reference
            ?: run {
                // Fallback to John 1. Attempt to use the first downloaded version.
                // If no versions have been downloaded, use NIV.
                val downloadedVersions = bibleVersionRepository.downloadedVersions
                val versionId = downloadedVersions.firstOrNull() ?: NIV_VERSION_ID
                BibleReference(
                    versionId = versionId,
                    bookUSFM = "JHN",
                    chapter = 1,
                )
            }
}
