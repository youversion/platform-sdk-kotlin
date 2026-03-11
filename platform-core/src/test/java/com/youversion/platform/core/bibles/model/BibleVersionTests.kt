package com.youversion.platform.core.bibles.model

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.helpers.FixtureLoader
import com.youversion.platform.helpers.YouVersionPlatformTest
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BibleVersionTests : YouVersionPlatformTest {
    lateinit var bibleVersion: BibleVersion
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setup() {
        val bible206Json = FixtureLoader().loadFixtureString("bible_206")
        val bible206IndexJson = FixtureLoader().loadFixtureString("bible_206_index")

        bibleVersion =
            BibleVersion.Builder.merge(
                json.decodeFromString(bible206Json),
                json.decodeFromString(bible206IndexJson),
            )
    }

    // ----- Core Metadata

    @Test
    fun `test decodes core metadata`() {
        assertEquals(206, bibleVersion.id)
        assertEquals("engWEBUS", bibleVersion.abbreviation)
        assertEquals("WEBUS", bibleVersion.localizedAbbreviation)
        assertEquals("World English Bible, American English Edition, without Strong's Numbers", bibleVersion.title)
        assertEquals(
            "World English Bible, American English Edition, without Strong's Numbers",
            bibleVersion.localizedTitle,
        )
        assertEquals("en", bibleVersion.languageTag)
        assertEquals(80, bibleVersion.books?.size)
        val bookCodes = bibleVersion.bookCodes
        assert(bookCodes != null)
        assertEquals(bookCodes, bibleVersion.books?.map { it.usfm })
        assertEquals("GEN", bookCodes?.first())
        assertEquals("REV", bookCodes?.last())
        assertEquals("PUBLIC DOMAIN (not copyrighted)", bibleVersion.copyright)
        assertEquals("This Public Domain Bible text is courtesy of eBible.org.", bibleVersion.promotionalContent)
    }

    // ----- displayTitle
    @Test
    fun `test displayTitle for single verse with abbreviation`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3, verse = 1)
        assertEquals("Genesis 3:1 WEBUS", bibleVersion.displayTitle(reference))
    }

    @Test
    fun `test displayTitle for single verse without abbreviation`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3, verse = 1)
        assertEquals("Genesis 3:1", bibleVersion.displayTitle(reference, includesVersionAbbreviation = false))
    }

    @Test
    fun `test displayTitle for verse range with abbreviation`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3, verseStart = 4, verseEnd = 6)
        assertEquals("Genesis 3:4-6 WEBUS", bibleVersion.displayTitle(reference))
    }

    @Test
    fun `test displayTitle for verse range without abbreviation`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3, verseStart = 4, verseEnd = 6)
        assertEquals("Genesis 3:4-6", bibleVersion.displayTitle(reference, includesVersionAbbreviation = false))
    }

    @Test
    fun `test displayTitle for chapter with abbreviation`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3)
        assertEquals("Genesis 3 WEBUS", bibleVersion.displayTitle(reference))
    }

    @Test
    fun `test displayTitle for chapter without abbreviation`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3)
        assertEquals("Genesis 3", bibleVersion.displayTitle(reference, includesVersionAbbreviation = false))
    }

    @Test
    fun `test displayTitle for rtl language`() {
        val bibleVersion = bibleVersion.copy(textDirection = "rtl")
        with(BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3)) {
            assertEquals("3 Genesis", bibleVersion.displayTitle(this, includesVersionAbbreviation = false))
        }

        with(BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3, verseStart = 4, verseEnd = 6)) {
            assertEquals("WEBUS 6-4:3 Genesis", bibleVersion.displayTitle(this))
        }
    }

    // ----- Book USFM Validation

    @Test
    fun `test book usfm validation`() {
        val canonicalUSFMs = bibleVersion.bookCodes?.map { it.uppercase() }?.toSet() ?: emptySet()

        val testCases =
            listOf(
                "GEN" to true,
                "gen" to true,
                "jHn" to true,
                "jhn" to true,
                "rev" to true,
                "GAN" to false,
                "REEV" to false,
                "R" to false,
            )

        for ((usfm, isValid) in testCases) {
            val normalized = usfm.uppercase()
            assertEquals(isValid, canonicalUSFMs.contains(normalized), "Expected $usfm to be $isValid")
        }
    }

    // ----- Books

    @Test
    fun `test decodes books`() {
        val books = bibleVersion.books
        assert(books != null)
        assertEquals(80, books?.size)

        val genesis = books?.first { it.usfm == "GEN" }
        assert(genesis != null)
        assertEquals("Gen", genesis?.abbreviation)
        assertEquals("Genesis", genesis?.title)

        val revelation = books?.first { it.usfm == "REV" }
        assert(revelation != null)
        assertEquals("Rev", revelation?.abbreviation)
        assertEquals("22", revelation?.chapters?.last()?.title)
    }

    @Test
    fun `test book metadata`() {
        data class BookTestCase(
            val bookUSFM: String,
            val expectedTitle: String,
            val expectedAbbreviation: String,
            val expectedChapterCount: Int,
            val expectedFirstChapterId: String,
        )

        val testCases =
            listOf(
                BookTestCase("GEN", "Genesis", "Gen", 50, "GEN.1"),
                BookTestCase("PSA", "Psalms", "Psa", 150, "PSA.1"),
                BookTestCase("REV", "Revelation", "Rev", 22, "REV.1"),
            )

        for (testCase in testCases) {
            val book = bibleVersion.book(testCase.bookUSFM)
            assert(book != null) { "Book ${testCase.bookUSFM} should exist" }
            assertEquals(testCase.expectedTitle, book?.title, "Title for ${testCase.bookUSFM}")
            assertEquals(testCase.expectedAbbreviation, book?.abbreviation, "Abbreviation for ${testCase.bookUSFM}")

            val chapters = book?.chapters
            assert(chapters != null) { "Chapters for ${testCase.bookUSFM} should exist" }
            assertEquals(testCase.expectedChapterCount, chapters?.size, "Chapter count for ${testCase.bookUSFM}")
            assertEquals(
                testCase.expectedFirstChapterId,
                chapters?.first()?.passageId,
                "First chapter ID for ${testCase.bookUSFM}",
            )

            val labels = bibleVersion.chapterLabels(testCase.bookUSFM)
            assertEquals(testCase.expectedChapterCount, labels.size, "Chapter labels count for ${testCase.bookUSFM}")
            assertEquals("1", labels.first(), "First chapter label for ${testCase.bookUSFM}")
            assertEquals(
                testCase.expectedChapterCount.toString(),
                labels.last(),
                "Last chapter label for ${testCase.bookUSFM}",
            )
        }
    }

    @Test
    fun `test book function`() {
        val genesis = bibleVersion.book("GEN")
        assertNotNull(genesis)
        assertEquals("Genesis", genesis.title)
        assertEquals("GEN", genesis.usfm)

        val nonExistent = bibleVersion.book("INVALID")
        assertNull(nonExistent)
    }

    @Test
    fun `test bookUSFMs returns non-null USFMs`() {
        val booksWithNullUsfm = bibleVersion.books!!.toMutableList()
        booksWithNullUsfm.add(BibleBook(null, null, null, null, null, null))

        val bibleVersion = bibleVersion.copy(books = booksWithNullUsfm)
        assertEquals(81, bibleVersion.books?.count())
        assertEquals(80, bibleVersion.bookUSFMs.size)
    }

    @Test
    fun `test bookUSFMs returns null if books is empty`() {
        val bibleVersion = bibleVersion.copy(books = null)
        assertEquals(emptyList(), bibleVersion.bookUSFMs)
    }

    @Test
    fun `test bookName returns the books title if found`() {
        assertEquals("Genesis", bibleVersion.bookName("GEN"))
        assertNull(bibleVersion.bookName("INVALID"))
    }

    // ----- Text Direction
    @Test
    fun `test isRightToLeft for left-to-right version`() {
        assertFalse { bibleVersion.isRightToLeft }
    }

    @Test
    fun `test isRightToLeft for right-to-left version`() {
        val bibleVersion = bibleVersion.copy(textDirection = "rtl")
        assertTrue { bibleVersion.isRightToLeft }
    }

    // ----- Reference

    @Test
    fun `test reference with valid single verse`() {
        val ref = bibleVersion.reference("GEN.1.1")
        assertNotNull(ref)
        assertEquals(206, ref.versionId)
        assertEquals("GEN", ref.bookUSFM)
        assertEquals(1, ref.chapter)
        assertEquals(1, ref.verseStart)
    }

    @Test
    fun `test reference with valid chapter only`() {
        val ref = bibleVersion.reference("PSA.23")
        assertNotNull(ref)
        assertEquals(206, ref.versionId)
        assertEquals("PSA", ref.bookUSFM)
        assertEquals(23, ref.chapter)
        assertEquals(1, ref.verseStart)
    }

    @Test
    fun `test reference with valid verse range`() {
        val ref = bibleVersion.reference("JHN.3.16-17")
        assertNotNull(ref)
        assertEquals(206, ref.versionId)
        assertEquals("JHN", ref.bookUSFM)
        assertEquals(3, ref.chapter)
        assertEquals(16, ref.verseStart)
        assertEquals(17, ref.verseEnd)
    }

    @Test
    fun `test reference trims whitespace`() {
        val ref = bibleVersion.reference("  GEN.1.1  ")
        assertNotNull(ref)
        assertEquals("GEN", ref.bookUSFM)
        assertEquals(1, ref.chapter)
        assertEquals(1, ref.verseStart)
    }

    @Test
    fun `test reference converts to uppercase`() {
        val ref = bibleVersion.reference("gen.1.1")
        assertNotNull(ref)
        assertEquals("GEN", ref.bookUSFM)
        assertEquals(1, ref.chapter)
        assertEquals(1, ref.verseStart)
    }

    @Test
    fun `test reference returns null for length less than 3`() {
        assertNull(bibleVersion.reference(""))
        assertNull(bibleVersion.reference("G"))
        assertNull(bibleVersion.reference("GE"))
        assertNull(bibleVersion.reference("  "))
    }

    @Test
    fun `test reference with plus sign merges references`() {
        val ref = bibleVersion.reference("GEN.1.1+GEN.1.2")
        assertNotNull(ref)
        assertEquals("GEN", ref.bookUSFM)
        assertEquals(1, ref.chapter)
        assertEquals(1, ref.verseStart)
        assertEquals(2, ref.verseEnd)
    }

    @Test
    fun `test reference with plus sign handles multiple ranges`() {
        val ref = bibleVersion.reference("PSA.23.1+PSA.23.2+PSA.23.3")
        assertNotNull(ref)
        assertEquals("PSA", ref.bookUSFM)
        assertEquals(23, ref.chapter)
        assertEquals(1, ref.verseStart)
        assertEquals(3, ref.verseEnd)
    }

    @Test
    fun `test reference returns null for invalid book usfm`() {
        assertNull(bibleVersion.reference("INVALID.1.1"))
        assertNull(bibleVersion.reference("XYZ.1.1"))
        assertNull(bibleVersion.reference("ABC.23"))
    }

    @Test
    fun `test reference returns null for non-existent book`() {
        assertNull(bibleVersion.reference("GAN.1.1"))
        assertNull(bibleVersion.reference("REEV.1.1"))
    }

    @Test
    fun `test reference handles malformed input gracefully`() {
        assertNull(bibleVersion.reference("..."))
        assertNull(bibleVersion.reference("GEN..1"))
    }

    // ----- chapterLabels

    @Test
    fun `test chapterLabels returns labels for known book`() {
        val labels = bibleVersion.chapterLabels("GEN")
        assertEquals(50, labels.size)
        assertEquals("1", labels.first())
        assertEquals("50", labels.last())
    }

    @Test
    fun `test chapterLabels returns empty list for unknown book`() {
        assertEquals(emptyList(), bibleVersion.chapterLabels("INVALID"))
    }

    @Test
    fun `test chapterLabels filters non-canonical chapters`() {
        val canonicalChapter =
            BibleChapter(
                id = "1",
                passageId = "TST.1",
                title = "1",
                verses = listOf(BibleVerse("1", "TST.1.1", "1")),
            )
        val nonCanonicalChapter =
            BibleChapter(
                id = "INTRO",
                passageId = "TST.INTRO",
                title = "Intro",
                verses = emptyList(),
            )
        val testBook =
            BibleBook(
                id = "TST",
                title = "Test",
                fullTitle = null,
                abbreviation = "Tst",
                canon = "new_testament",
                chapters = listOf(nonCanonicalChapter, canonicalChapter),
            )
        val version = bibleVersion.copy(books = listOf(testBook))

        val labels = version.chapterLabels("TST")
        assertEquals(listOf("1"), labels)
    }

    @Test
    fun `test chapterLabels excludes chapters with null title`() {
        val chapterWithNullTitle =
            BibleChapter(
                id = "1",
                passageId = "TST.1",
                title = null,
                verses = listOf(BibleVerse("1", "TST.1.1", "1")),
            )
        val chapterWithTitle =
            BibleChapter(
                id = "2",
                passageId = "TST.2",
                title = "2",
                verses = listOf(BibleVerse("1", "TST.2.1", "1")),
            )
        val testBook =
            BibleBook(
                id = "TST",
                title = "Test",
                fullTitle = null,
                abbreviation = "Tst",
                canon = "new_testament",
                chapters = listOf(chapterWithNullTitle, chapterWithTitle),
            )
        val version = bibleVersion.copy(books = listOf(testBook))

        val labels = version.chapterLabels("TST")
        assertEquals(listOf("2"), labels)
    }

    @Test
    fun `test chapterLabels for single-chapter book`() {
        val labels = bibleVersion.chapterLabels("JUD")
        assertEquals(1, labels.size)
        assertEquals("1", labels.first())
    }

    // ----- canonicalChapters

    @Test
    fun `test canonicalChapters returns all chapters for fully canonical book`() {
        val chapters = bibleVersion.canonicalChapters("GEN")
        assertEquals(50, chapters.size)
    }

    @Test
    fun `test canonicalChapters returns empty list for unknown book`() {
        assertEquals(emptyList(), bibleVersion.canonicalChapters("INVALID"))
    }

    @Test
    fun `test canonicalChapters filters non-canonical chapters`() {
        val canonicalChapter =
            BibleChapter(
                id = "1",
                passageId = "TST.1",
                title = "1",
                verses = listOf(BibleVerse("1", "TST.1.1", "1")),
            )
        val nonCanonicalChapter =
            BibleChapter(
                id = "INTRO",
                passageId = "TST.INTRO",
                title = "Intro",
                verses = emptyList(),
            )
        val testBook =
            BibleBook(
                id = "TST",
                title = "Test",
                fullTitle = null,
                abbreviation = "Tst",
                canon = "new_testament",
                chapters = listOf(nonCanonicalChapter, canonicalChapter),
            )
        val version = bibleVersion.copy(books = listOf(testBook))

        val chapters = version.canonicalChapters("TST")
        assertEquals(1, chapters.size)
        assertEquals("1", chapters.first().id)
    }

    @Test
    fun `test canonicalChapters for single-chapter book`() {
        val chapters = bibleVersion.canonicalChapters("JUD")
        assertEquals(1, chapters.size)
    }

    // ----- equals and hashCode

    @Test
    fun `test equals returns true for same instance`() {
        assertTrue { bibleVersion.equals(bibleVersion) }
    }

    @Test
    fun `test equals returns true for same id with different fields`() {
        val other = BibleVersion(id = 206, abbreviation = "OTHER", title = "Different Title")
        assertTrue { bibleVersion.equals(other) }
    }

    @Test
    fun `test equals returns false for different id`() {
        val other = BibleVersion(id = 1, abbreviation = "KJV")
        assertFalse { bibleVersion.equals(other) }
    }

    @Test
    fun `test equals returns false for non-BibleVersion`() {
        assertFalse { bibleVersion.equals("not a BibleVersion") }
        assertFalse { bibleVersion.equals(206) }
        assertFalse { bibleVersion.equals(null) }
    }

    @Test
    fun `test hashCode is consistent for same id`() {
        val other = BibleVersion(id = 206, abbreviation = "OTHER")
        assertEquals(bibleVersion.hashCode(), other.hashCode())
    }

    @Test
    fun `test hashCode differs for different id`() {
        val other = BibleVersion(id = 1)
        assertFalse { bibleVersion.hashCode() == other.hashCode() }
    }

    // ----- displayTitle for single-chapter book (JUD)

    @Test
    fun `test displayTitle for single-chapter book chapter only`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "JUD", chapter = 1)
        assertEquals("Jude WEBUS", bibleVersion.displayTitle(reference))
    }

    @Test
    fun `test displayTitle for single-chapter book chapter only without abbreviation`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "JUD", chapter = 1)
        assertEquals("Jude", bibleVersion.displayTitle(reference, includesVersionAbbreviation = false))
    }

    @Test
    fun `test displayTitle for single-chapter book single verse`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "JUD", chapter = 1, verse = 5)
        assertEquals("Jude 5 WEBUS", bibleVersion.displayTitle(reference))
    }

    @Test
    fun `test displayTitle for single-chapter book single verse without abbreviation`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "JUD", chapter = 1, verse = 5)
        assertEquals("Jude 5", bibleVersion.displayTitle(reference, includesVersionAbbreviation = false))
    }

    @Test
    fun `test displayTitle for single-chapter book verse range`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "JUD", chapter = 1, verseStart = 3, verseEnd = 5)
        assertEquals("Jude 3-5 WEBUS", bibleVersion.displayTitle(reference))
    }

    @Test
    fun `test displayTitle for single-chapter book verse range without abbreviation`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "JUD", chapter = 1, verseStart = 3, verseEnd = 5)
        assertEquals("Jude 3-5", bibleVersion.displayTitle(reference, includesVersionAbbreviation = false))
    }

    @Test
    fun `test displayTitle for single-chapter book rtl`() {
        val rtlVersion = bibleVersion.copy(textDirection = "rtl")
        val reference = BibleReference(versionId = 206, bookUSFM = "JUD", chapter = 1, verse = 5)
        assertEquals("WEBUS 5 Jude", rtlVersion.displayTitle(reference))
    }

    // ----- displayTitle for single verse via range (verseStart == verseEnd)

    @Test
    fun `test displayTitle for verse range where start equals end`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3, verseStart = 16, verseEnd = 16)
        assertEquals("Genesis 3:16 WEBUS", bibleVersion.displayTitle(reference))
    }

    @Test
    fun `test displayTitle for verse range where start equals end without abbreviation`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3, verseStart = 16, verseEnd = 16)
        assertEquals("Genesis 3:16", bibleVersion.displayTitle(reference, includesVersionAbbreviation = false))
    }

    // ----- displayTitle edge cases

    @Test
    fun `test displayTitle omits abbreviation when both are null`() {
        val version = bibleVersion.copy(localizedAbbreviation = null, abbreviation = null)
        val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3, verse = 1)
        assertEquals("Genesis 3:1", version.displayTitle(reference))
    }

    @Test
    fun `test displayTitle for whole chapter with verseEnd 999`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3, verseStart = 1, verseEnd = 999)
        assertEquals("Genesis 3 WEBUS", bibleVersion.displayTitle(reference))
    }

    @Test
    fun `test displayTitle for single-chapter book with verseEnd 999`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "JUD", chapter = 1, verseStart = 1, verseEnd = 999)
        assertEquals("Jude WEBUS", bibleVersion.displayTitle(reference))
    }

    @Test
    fun `test displayTitle for verse with no verseEnd`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 3, verseStart = 16, verseEnd = null)
        assertEquals("Genesis 3:16 WEBUS", bibleVersion.displayTitle(reference))
    }

    @Test
    fun `test displayTitle for single-chapter book verse with no verseEnd`() {
        val reference = BibleReference(versionId = 206, bookUSFM = "JUD", chapter = 1, verseStart = 5, verseEnd = null)
        assertEquals("Jude 5 WEBUS", bibleVersion.displayTitle(reference))
    }
}
