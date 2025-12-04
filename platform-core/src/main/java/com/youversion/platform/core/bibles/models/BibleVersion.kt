package com.youversion.platform.core.bibles.models

import com.youversion.platform.core.bibles.domain.BibleReference
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleVersion(
    @SerialName(CodingKey.ID) val id: Int,
    @SerialName(CodingKey.ABBREVIATION) val abbreviation: String? = null,
    @SerialName(CodingKey.COPYRIGHT_LONG) val copyrightLong: String? = null,
    @SerialName(CodingKey.COPYRIGHT_SHORT) val copyrightShort: String? = null,
    @SerialName(CodingKey.LANGUAGE_TAG) val languageTag: String? = null,
    @SerialName(CodingKey.LOCALIZED_ABBREVIATION) val localizedAbbreviation: String? = null,
    @SerialName(CodingKey.LOCALIZED_TITLE) val localizedTitle: String? = null,
    @SerialName(CodingKey.READER_FOOTER) val readerFooter: String? = null,
    @SerialName(CodingKey.READER_FOOTER_URL) val readerFooterUrl: String? = null,
    @SerialName(CodingKey.TITLE) val title: String? = null,
    @SerialName(CodingKey.BOOK_CODES) val bookCodes: List<String>? = null,
    @SerialName(CodingKey.BOOKS) val books: List<BibleBook>? = null,
    @SerialName(CodingKey.TEXT_DIRECTION) val textDirection: String? = null,
    @SerialName(CodingKey.ORGANIZATION_ID) val organizationId: String? = null,
) {
    private object CodingKey {
        const val ID = "id"
        const val ABBREVIATION = "abbreviation"
        const val BOOK_CODES = "books"
        const val BOOKS = "BibleBooks" // This will be merged from /index but we need to encode it for caching
        const val COPYRIGHT_LONG = "copyright_long"
        const val COPYRIGHT_SHORT = "copyright_short"
        const val LANGUAGE_TAG = "language_tag"
        const val LOCALIZED_ABBREVIATION = "local_abbreviation"
        const val LOCALIZED_TITLE = "local_title"
        const val READER_FOOTER = "info"
        const val READER_FOOTER_URL = "publisher_url"
        const val TEXT_DIRECTION = "text_direction"
        const val TITLE = "title"
        const val ORGANIZATION_ID = "organization_id"
    }

    val isRightToLeft: Boolean
        get() = textDirection == "rtl"

    fun book(usfm: String): BibleBook? = books?.find { it.usfm == usfm }

    val bookUSFMs: List<String>
        get() = books?.let { it.mapNotNull { book -> book.usfm } } ?: emptyList()

    fun bookName(bookUsfm: String): String? = book(bookUsfm)?.title

    fun reference(usfm: String): BibleReference? {
        val trimmedUSFM = usfm.trim().uppercase()
        if (trimmedUSFM.length < 3) {
            return null
        }

        // if there's a range indicated with + characters, return the first valid range:
        val subUSFMs = trimmedUSFM.split("+")
        if (subUSFMs.size > 1) {
            val refs = subUSFMs.mapNotNull { reference(it) }
            val merged = BibleReference.referencesByMerging(refs)
            return merged.firstOrNull()
        }

        val reference = BibleReference.unvalidatedReference(trimmedUSFM, id) ?: return null
        if (!isBookUSFMValid(reference.bookUSFM)) {
            return null
        }
        return reference
        // TODO: check that the chapter and verse numbers are valid for this version.
    }

    private fun isBookUSFMValid(usfm: String): Boolean {
        val books = books ?: return false
        val usfmUpper = usfm.uppercase()
        return books.any { it.usfm == usfmUpper }
    }

    fun chapterLabels(bookUsfm: String): List<String> =
        book(bookUsfm)
            ?.chapters
            ?.filter { it.isCanonical != false }
            ?.mapNotNull { it.title }
            ?: emptyList()

    fun canonicalChapters(bookUsfm: String): List<BibleChapter> =
        book(bookUsfm)
            ?.chapters
            ?.filter { it.isCanonical == true }
            ?: emptyList()

    // Example: "https://www.bible.com/bible/111/1SA.3.10.NIV"
    fun shareUrl(reference: BibleReference): String? {
        val prefix = "https://www.bible.com/bible/$id/"
        val book = reference.bookUSFM
        val version =
            localizedAbbreviation?.takeIf { it.isNotEmpty() }
                ?: abbreviation?.takeIf { it.isNotEmpty() }
                ?: id.toString()

        val urlString =
            if (reference.verseStart != null) {
                val verseStart = reference.verseStart
                if (reference.verseEnd != null && verseStart != reference.verseEnd) {
                    "$prefix$book.${reference.chapter}.$verseStart-${reference.verseEnd}.$version"
                } else {
                    "$prefix$book.${reference.chapter}.$verseStart.$version"
                }
            } else {
                "$prefix$book.${reference.chapter}.$version"
            }

        return urlString
    }

    /**
     * Creates a display title for a Bible reference, optionally including the version abbreviation.
     *
     * @param reference The Bible reference to format
     * @param includesVersionAbbreviation Whether to append the version abbreviation (default: true)
     * @return A formatted display title (e.g., "John 3:16 KJV" or "يوحنا 16:3 KJV" for RTL)
     */
    fun displayTitle(
        reference: BibleReference,
        includesVersionAbbreviation: Boolean = true,
    ): String {
        var referenceOnlyChunks = titleChunks(reference)
        if (isRightToLeft) {
            referenceOnlyChunks = referenceOnlyChunks.reversed()
        }
        val referenceOnlyTitle = referenceOnlyChunks.joinToString("")
        val titleChunks = mutableListOf(referenceOnlyTitle)

        if (includesVersionAbbreviation) {
            val abbreviation = localizedAbbreviation ?: abbreviation
            if (abbreviation != null) {
                titleChunks.add(abbreviation)
                if (isRightToLeft) {
                    titleChunks.reverse()
                }
            }
        }

        return titleChunks.joinToString(" ")
    }

    /**
     * Generates title chunks for a Bible reference (e.g., ["John", " ", "3", ":", "16", "-", "17"]).
     * These chunks can be reversed for RTL languages and joined to form a display title.
     */
    private fun titleChunks(reference: BibleReference): List<String> {
        val bookUSFM = reference.bookUSFM
        val bookName = book(bookUSFM)?.title ?: ""

        val hasOneChapter = canonicalChapters(bookUSFM).count() == 1
        val chapterSeparator = if (hasOneChapter) " " else ":"
        val bookAndChapterSeparator = if (hasOneChapter) "" else " "
        val chapter = if (hasOneChapter) "" else reference.chapter.toString()

        val verseStart = reference.verseStart
        val verseEnd = reference.verseEnd

        return when {
            // Whole chapter (verseEnd is 999)
            verseEnd == 999 -> {
                listOf(bookName, bookAndChapterSeparator, chapter)
            }
            // Whole chapter (no verses specified)
            verseStart == null -> {
                listOf(bookName, bookAndChapterSeparator, chapter)
            }
            // Single verse (both start and end are the same)
            verseEnd != null && verseStart == verseEnd -> {
                listOf(bookName, bookAndChapterSeparator, chapter, chapterSeparator, verseStart.toString())
            }
            // Verse range (different start and end)
            verseEnd != null -> {
                listOf(
                    bookName,
                    bookAndChapterSeparator,
                    chapter,
                    chapterSeparator,
                    verseStart.toString(),
                    "-",
                    verseEnd.toString(),
                )
            }
            // Single verse with no verseEnd
            else -> {
                listOf(bookName, bookAndChapterSeparator, chapter, chapterSeparator, verseStart.toString())
            }
        }
    }

    // ----- Equality
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BibleVersion) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    // ----- Companion
    companion object {
        val preview: BibleVersion
            get() {
                val copyrightLong =
                    """
                    King James Version (KJV)

                    The King James Version (KJV) of the holy Bible was first printed in 1611, but the main edition used today is the 1769 version. The King James Version (KJV) is also known as the Authorized (or Authorised) Version (AV) because it was authorized to be read in churches. For over 300 years it was the main English translation used in the English speaking world, and is much admired and respected. About 400 words and phrases coined or popularised by the King James Version are part the English language today.
                    """.trimIndent()

                return BibleVersion(
                    id = 1,
                    abbreviation = "KJV",
                    copyrightLong = copyrightLong,
                    copyrightShort = null,
                    languageTag = "en",
                    localizedAbbreviation = "KJV",
                    localizedTitle = "King James Version",
                    readerFooter = "Text is from the King James Version",
                    readerFooterUrl = "https://www.biblesociety.org.uk",
                    title = "King James Version",
                    bookCodes = null,
                )
            }
    }

    object Builder {
        fun merge(
            basic: BibleVersion,
            index: BibleVersionIndex,
        ): BibleVersion {
            val books =
                index.books
                    ?.filter { it.chapters != null && it.chapters.isNotEmpty() }
                    ?.map { book ->
                        // non-canonical chapters don't have verses.
                        val chapters =
                            book.chapters
                                ?.filter { it.verses != null && it.verses.isNotEmpty() }
                                ?.map { chapter ->
                                    BibleChapter(
                                        id = chapter.title,
                                        bookUSFM = chapter.id,
                                        isCanonical = true,
                                        passageId = chapter.id,
                                        title = chapter.title,
                                    )
                                }

                        BibleBook(
                            usfm = book.id,
                            abbreviation = book.abbreviation,
                            title = book.title,
                            canon = book.canon,
                            chapters = chapters,
                        )
                    }

            return basic.copy(
                bookCodes = books?.mapNotNull { it.usfm },
                books = books,
                textDirection = index.textDirection,
            )
        }
    }
}
