package com.youversion.platform.reader.domain

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.domain.Storage
import kotlinx.serialization.json.Json
import java.text.Collator
import java.util.Locale

/**
 * Responsible for fetching and managing data related to the Bible
 * Reader. Note that versions are used by the Bible Reader, but those
 * are managed by the BibleVersionRepository.
 */
class BibleReaderRepository(
    private val storage: Storage,
    private val bibleVersionRepository: BibleVersionRepository,
    private val globalState: BibleReaderGlobalState,
) {
    companion object {
        private const val NIV_VERSION_ID = 111
        private const val KEY_BIBLE_READER_REFERENCE = "bible-reader-view--reference"
    }

    val localeCountryCode: String
        get() = Locale.getDefault().country ?: "US"
    val localeLanguageCode: String
        get() = Locale.getDefault().language ?: "en"

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

    fun previousChapter(
        version: BibleVersion?,
        bibleReference: BibleReference,
    ): BibleReference? {
        val books = version?.books ?: emptyList()
        val previousBookIndex =
            books.indexOfFirst { it.id == bibleReference.bookUSFM }
        return when {
            bibleReference.chapter > 1 -> {
                // We're navigating to a previous chapter inside the same book
                bibleReference.copy(chapter = bibleReference.chapter - 1)
            }

            previousBookIndex > 0 -> {
                // We're navigating to the last chapter in the previous book
                val previousBook = books[previousBookIndex - 1]
                val lastChapter = previousBook.chapters?.count() ?: 0
                bibleReference.copy(
                    bookUSFM = previousBook.id ?: "",
                    chapter = lastChapter,
                )
            }

            else -> {
                // We're at the first chapter, intro, etc of the first book (e.g. Genesis 1)
                null
            }
        }
    }

    fun nextChapter(
        version: BibleVersion?,
        bibleReference: BibleReference,
    ): BibleReference? {
        val books = version?.books ?: emptyList()
        val currentBookIndex = books.indexOfFirst { it.id == bibleReference.bookUSFM }
        val currentBook = books.getOrNull(currentBookIndex)
        val lastChapter = currentBook?.chapters?.count() ?: 0

        return when {
            bibleReference.chapter < lastChapter -> {
                // We're navigating to the next chapter in the same book
                bibleReference.copy(chapter = bibleReference.chapter + 1)
            }

            currentBookIndex < books.count() - 1 -> {
                // We're navigating to the first chapter of the next book
                val nextBook = books.getOrNull(currentBookIndex + 1)
                bibleReference.copy(
                    bookUSFM = nextBook?.id ?: "",
                    chapter = 1,
                )
            }

            else -> {
                // We're at the end of the last book
                null
            }
        }
    }

    /** In-memory cache of bible versions which have been fetched by language */
    private var versionsInLanguage: MutableMap<String, List<BibleVersion>> = mutableMapOf()

    /** Holds minimal information about all Bible versions available to this app, in all languages. */
    var permittedVersions: List<BibleVersion>? = null
        private set

    /**
     * Returns minimal information about all Bible versions available to this app, in all languages
     */
    suspend fun permittedVersionsListing(): List<BibleVersion> =
        permittedVersions
            ?: bibleVersionRepository
                .permittedVersions()
                .also { permittedVersions = it }

    /**
     * Returns complete information about Bible versions available in a specific language.
     */
    suspend fun fetchVersionsInLanguage(languageCode: String): List<BibleVersion> {
        // Check if we already have the versions for this language locally
        if (!versionsInLanguage[languageCode].isNullOrEmpty()) {
            return versionsInLanguage[languageCode] ?: emptyList()
        }

        // There is currently no language with more than 99 versions so ignore pagination for now
        val unsortedVersions =
            YouVersionApi.bible
                .versions(languageCode = languageCode, pageSize = 99)
                .data

        fun comparableString(bibleVersion: BibleVersion): String =
            bibleVersion.localizedTitle ?: bibleVersion.title ?: bibleVersion.localizedAbbreviation
                ?: bibleVersion.abbreviation
                ?: bibleVersion.id.toString()

        // collator allows for locale-specific string comparisons
        val collator = Collator.getInstance()
        return unsortedVersions
            .distinctBy { it.id }
            .sortedWith { a, b ->
                val aTitle = comparableString(a).lowercase()
                val bTitle = comparableString(b).lowercase()
                collator.compare(aTitle, bTitle)
            }.also { globalState.update { s -> s.copy(permittedVersions = it) } }
    }
}
