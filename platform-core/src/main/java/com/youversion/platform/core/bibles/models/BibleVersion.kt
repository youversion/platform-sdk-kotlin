package com.youversion.platform.core.bibles.models

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class BibleVersion
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.ID) public val id: Int,
        @SerialName(CodingKey.ABBREVIATION) public val abbreviation: String? = null,
        @SerialName(CodingKey.PROMOTIONAL_CONTENT) public val promotionalContent: String? = null,
        @SerialName(CodingKey.COPYRIGHT) public val copyright: String? = null,
        @SerialName(CodingKey.LANGUAGE_TAG) public val languageTag: String? = null,
        @SerialName(CodingKey.LOCALIZED_ABBREVIATION) public val localizedAbbreviation: String? = null,
        @SerialName(CodingKey.LOCALIZED_TITLE) public val localizedTitle: String? = null,
        @SerialName(CodingKey.READER_FOOTER) public val readerFooter: String? = null,
        @SerialName(CodingKey.READER_FOOTER_URL) public val readerFooterUrl: String? = null,
        @SerialName(CodingKey.TITLE) public val title: String? = null,
        @SerialName(CodingKey.BOOK_CODES) public val bookCodes: List<String>? = null,
        @SerialName(CodingKey.BOOKS) public val books: List<BibleBook>? = null,
        @SerialName(CodingKey.TEXT_DIRECTION) public val textDirection: String? = null,
        @SerialName(CodingKey.ORGANIZATION_ID) public val organizationId: String? = null,
    ) {
        public object CodingKey {
            public const val ID: String = "id"
            public const val ABBREVIATION: String = "abbreviation"
            public const val BOOK_CODES: String = "books"

            // This will be merged from /index but we need to encode it for caching
            public const val BOOKS: String = "BibleBooks"
            public const val PROMOTIONAL_CONTENT: String = "promotional_content"
            public const val COPYRIGHT: String = "copyright"
            public const val LANGUAGE_TAG: String = "language_tag"
            public const val LOCALIZED_ABBREVIATION: String = "localized_abbreviation"
            public const val LOCALIZED_TITLE: String = "localized_title"
            public const val READER_FOOTER: String = "info"
            public const val READER_FOOTER_URL: String = "publisher_url"
            public const val TEXT_DIRECTION: String = "text_direction"
            public const val TITLE: String = "title"
            public const val ORGANIZATION_ID: String = "organization_id"
        }

        public val isRightToLeft: Boolean
            get() = textDirection == "rtl"

        public fun book(usfm: String): BibleBook? = books?.find { it.usfm == usfm }

        public val bookUSFMs: List<String>
            get() = books?.let { it.mapNotNull { book -> book.usfm } } ?: emptyList()

        public fun bookName(bookUsfm: String): String? = book(bookUsfm)?.title

        public fun reference(usfm: String): BibleReference? {
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

        public fun chapterLabels(bookUsfm: String): List<String> =
            book(bookUsfm)
                ?.chapters
                ?.filter { it.isCanonical != false }
                ?.mapNotNull { it.title }
                ?: emptyList()

        public fun canonicalChapters(bookUsfm: String): List<BibleChapter> =
            book(bookUsfm)
                ?.chapters
                ?.filter { it.isCanonical == true }
                ?: emptyList()

        // Example: "https://www.bible.com/bible/111/1SA.3.10.NIV"
        public fun shareUrl(reference: BibleReference): String? {
            val prefix = "https://www.bible.com/bible/$id/"
            val book = reference.bookUSFM
            val version =
                localizedAbbreviation?.takeIf { it.isNotEmpty() }
                    ?: abbreviation?.takeIf { it.isNotEmpty() }
                    ?: id.toString()

            val verseStart = reference.verseStart
            val verseEnd = reference.verseEnd
            val chapterUrl = "$prefix$book.${reference.chapter}.$version"

            return when {
                verseEnd == WHOLE_CHAPTER_VERSE_END || verseStart == null -> {
                    chapterUrl
                }

                reference.isRange -> {
                    "$prefix$book.${reference.chapter}.$verseStart-$verseEnd.$version"
                }

                else -> {
                    "$prefix$book.${reference.chapter}.$verseStart.$version"
                }
            }
        }

        /**
         * Creates a display title for a Bible reference, optionally including the version abbreviation.
         *
         * @param reference The Bible reference to format
         * @param includesVersionAbbreviation Whether to append the version abbreviation (default: true)
         * @return A formatted display title (e.g., "John 3:16 KJV" or "يوحنا 16:3 KJV" for RTL)
         */
        public fun displayTitle(
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
                // Whole chapter, either as the platform marks it or with no verses specified
                verseEnd == WHOLE_CHAPTER_VERSE_END || verseStart == null -> {
                    listOf(bookName, bookAndChapterSeparator, chapter)
                }

                // Single verse (both start and end are the same)
                !reference.isRange -> {
                    listOf(bookName, bookAndChapterSeparator, chapter, chapterSeparator, verseStart.toString())
                }

                // Verse range (different start and end)
                else -> {
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
            }
        }

        // ----- Equality
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BibleVersion) return false
            return id == other.id
        }

        override fun hashCode(): Int = id.hashCode()

        override fun toString(): String =
            "BibleVersion(id=$id, abbreviation=$abbreviation, promotionalContent=$promotionalContent, " +
                "copyright=$copyright, languageTag=$languageTag, localizedAbbreviation=$localizedAbbreviation, " +
                "localizedTitle=$localizedTitle, readerFooter=$readerFooter, readerFooterUrl=$readerFooterUrl, " +
                "title=$title, bookCodes=$bookCodes, books=$books, textDirection=$textDirection, " +
                "organizationId=$organizationId)"

        // ----- Companion
        public companion object {
            /** The end verse the platform uses to mean "the whole chapter" rather than a real verse number. */
            private const val WHOLE_CHAPTER_VERSE_END = 999

            public val preview: BibleVersion
                get() {
                    val copyrightLong =
                        """
                        King James Version (KJV)

                        The King James Version (KJV) of the holy Bible was first printed in 1611, but the main edition used today is the 1769 version. The King James Version (KJV) is also known as the Authorized (or Authorised) Version (AV) because it was authorized to be read in churches. For over 300 years it was the main English translation used in the English speaking world, and is much admired and respected. About 400 words and phrases coined or popularised by the King James Version are part the English language today.
                        """.trimIndent()

                    return BibleVersion(
                        id = 1,
                        abbreviation = "KJV",
                        promotionalContent = copyrightLong,
                        copyright = null,
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

        internal object Builder {
            fun merge(
                basic: BibleVersion,
                index: BibleVersionIndex,
            ): BibleVersion =
                BibleVersion(
                    id = basic.id,
                    abbreviation = basic.abbreviation,
                    promotionalContent = basic.promotionalContent,
                    copyright = basic.copyright,
                    languageTag = basic.languageTag,
                    localizedAbbreviation = basic.localizedAbbreviation,
                    localizedTitle = basic.localizedTitle,
                    readerFooter = basic.readerFooter,
                    readerFooterUrl = basic.readerFooterUrl,
                    title = basic.title,
                    bookCodes = basic.bookCodes,
                    books = index.books,
                    textDirection = index.textDirection,
                    organizationId = basic.organizationId,
                )
        }
    }
