package com.youversion.platform.reader.domain

import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.core.languages.models.Language
import kotlinx.serialization.json.Json
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
                // If no versions have been downloaded, use BSB.
                val downloadedVersions = bibleVersionRepository.downloadedVersions
                val versionId = downloadedVersions.firstOrNull() ?: BibleDefaults.VERSION_ID
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
                val chapters = previousBook.chapters ?: return null
                val lastChapter = chapters.count()
                if (lastChapter < 1) {
                    return null
                }
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

    // ----- Languages
    val allPermittedLanguageTags: List<String>
        get() =
            bibleVersionRepository.permittedVersions
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

        val result =
            bibleVersionRepository.permittedVersions?.let { permittedVersions ->
                codes
                    .filter { languageCode ->
                        permittedVersions.isEmpty() ||
                            permittedVersions.any { it.languageTag == languageCode }
                    }
            } ?: codes
        suggestedLanguageTags = result
        return result
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

    fun languageName(lang: String): String {
        languageNames[lang]?.let { return it }
        val displayName = Locale.forLanguageTag(lang).getDisplayLanguage(Locale.getDefault())
        return displayName.takeIf { it.isNotBlank() } ?: lang
    }

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
