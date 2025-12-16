package com.youversion.platform.core.bibles.model

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleBook
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
}
