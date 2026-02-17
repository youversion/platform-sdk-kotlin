package com.youversion.platform.reader.domain

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.core.languages.models.Language
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
    private val languageRepository: LanguageRepository,
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
            try {
                storage
                    .getStringOrNull(KEY_BIBLE_READER_REFERENCE)
                    ?.let { Json.decodeFromString(it) }
            } catch (_: Exception) {
                null
            }
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
                    chapter = "1",
                )
            }

    fun previousChapter(
        version: BibleVersion?,
        bibleReference: BibleReference,
    ): BibleReference? {
        val books = version?.books ?: emptyList()
        val currentBookIndex =
            books.indexOfFirst { it.id == bibleReference.bookUSFM }
        val currentBook = books.getOrNull(currentBookIndex)
        val currentChapter = bibleReference.chapterNumber

        return when {
            bibleReference.isIntro -> {
                if (currentBookIndex > 0) {
                    val previousBook = books[currentBookIndex - 1]
                    val lastChapter = previousBook.chapters?.count() ?: 0
                    bibleReference.copy(
                        bookUSFM = previousBook.id ?: "",
                        chapter = lastChapter.toString(),
                    )
                } else {
                    null
                }
            }

            currentChapter != null && currentChapter > 1 -> {
                bibleReference.copy(chapter = (currentChapter - 1).toString())
            }

            currentChapter == 1 && currentBook?.hasIntro == true -> {
                bibleReference.copy(chapter = BibleReference.INTRO)
            }

            currentBookIndex > 0 -> {
                val previousBook = books[currentBookIndex - 1]
                val lastChapter = previousBook.chapters?.count() ?: 0
                bibleReference.copy(
                    bookUSFM = previousBook.id ?: "",
                    chapter = lastChapter.toString(),
                )
            }

            else -> {
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
        val currentChapter = bibleReference.chapterNumber

        return when {
            bibleReference.isIntro -> {
                bibleReference.copy(chapter = "1")
            }

            currentChapter != null && currentChapter < lastChapter -> {
                bibleReference.copy(chapter = (currentChapter + 1).toString())
            }

            currentBookIndex < books.count() - 1 -> {
                val nextBook = books.getOrNull(currentBookIndex + 1)
                val nextChapter =
                    if (nextBook?.hasIntro == true) BibleReference.INTRO else "1"
                bibleReference.copy(
                    bookUSFM = nextBook?.id ?: "",
                    chapter = nextChapter,
                )
            }

            else -> {
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
            }
    }

    // ----- Languages
    val allPermittedLanguageTags: List<String>
        get() =
            permittedVersions
                ?.mapNotNull { it.languageTag }
                ?.distinct()
                ?: emptyList()

    private var suggestedLanguageTags: List<String>? = null

    suspend fun suggestedLanguageTags(): List<String> {
        if (!suggestedLanguageTags.isNullOrEmpty()) {
            return suggestedLanguageTags!!
        }

        val data = languageRepository.suggestedLanguages(localeCountryCode)
        val codes = if (data.isEmpty()) listOf("en", "es") else extractLanguageCodes(data)

        return permittedVersions?.let { permittedVersions ->
            codes
                .filter { languageCode ->
                    permittedVersions.isEmpty() ||
                        permittedVersions.any { it.languageTag == languageCode }
                }
        } ?: codes
    }

    private fun extractLanguageCodes(languages: List<Language>): List<String> =
        languages
            .mapNotNull { it.language }
            .distinct()

    private var languageNames: Map<String, String> = emptyMap()

    suspend fun loadLanguageNames(version: BibleVersion?) {
        if (languageNames.isNotEmpty()) return

        val result =
            languageRepository
                .languages()

        val langNames =
            result
                .mapNotNull { language ->
                    language.language?.let { code ->
                        language.displayNames?.let { displayNames ->
                            bestDisplayName(displayNames, version)?.let { name ->
                                code to name
                            }
                        }
                    }
                }.toMap()

        languageNames = langNames
    }

    fun languageName(lang: String): String =
        languageNames[lang]
            ?: Locale.getDefault().getDisplayLanguage(Locale(lang))
            ?: lang

    private fun bestDisplayName(
        names: Map<String, String?>,
        version: BibleVersion?,
    ): String? {
        if (names.isEmpty()) return null
        if (names.size < 2) return names.entries.firstOrNull()?.value

        names[localeLanguageCode]?.let { return it }
        version?.languageTag?.let { names[it] }?.let { return it }
        names["en"]?.let { return it }

        return names.entries.firstOrNull()?.value
    }
}
