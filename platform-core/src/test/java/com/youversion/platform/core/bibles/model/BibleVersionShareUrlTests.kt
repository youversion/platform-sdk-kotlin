package com.youversion.platform.core.bibles.model

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlin.test.Test
import kotlin.test.assertEquals

class BibleVersionShareUrlTests {
    private fun createBibleVersion(
        id: Int,
        abbreviation: String? = null,
        localizedAbbreviation: String? = null,
    ): BibleVersion =
        BibleVersion(
            id = id,
            abbreviation = abbreviation,
            copyrightLong = null,
            copyrightShort = null,
            languageTag = "eng",
            localizedAbbreviation = localizedAbbreviation,
            localizedTitle = null,
            readerFooter = null,
            readerFooterUrl = null,
            title = null,
            bookCodes = null,
            books = null,
            textDirection = "ltr",
        )

    // ----- Single Verse Tests

    @Test
    fun `test shareUrl single verse`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV")
        val reference = BibleReference(versionId = 111, bookUSFM = "1SA", chapter = 3, verse = 10)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/1SA.3.10.NIV", url)
    }

    @Test
    fun `test shareUrl single verse with localized abbreviation`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV", localizedAbbreviation = "NVI")
        val reference = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verse = 1)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/GEN.1.1.NVI", url)
    }

    @Test
    fun `test shareUrl single verse no abbreviation`() {
        val version = createBibleVersion(id = 999)
        val reference = BibleReference(versionId = 999, bookUSFM = "PSA", chapter = 23, verse = 1)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/999/PSA.23.1.999", url)
    }

    // ----- Verse Range Tests

    @Test
    fun `test shareUrl verse range`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV")
        val reference = BibleReference(versionId = 111, bookUSFM = "1SA", chapter = 3, verseStart = 10, verseEnd = 15)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/1SA.3.10-15.NIV", url)
    }

    @Test
    fun `test shareUrl verse range with localized abbreviation`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV", localizedAbbreviation = "NVI")
        val reference = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 5)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/GEN.1.1-5.NVI", url)
    }

    @Test
    fun `test shareUrl verse range same start and end`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV")
        val reference = BibleReference(versionId = 111, bookUSFM = "1SA", chapter = 3, verseStart = 10, verseEnd = 10)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/1SA.3.10.NIV", url)
    }

    // ----- Chapter Only Tests

    @Test
    fun `test shareUrl chapter only`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV")
        val reference = BibleReference(versionId = 111, bookUSFM = "1SA", chapter = 3)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/1SA.3.NIV", url)
    }

    @Test
    fun `test shareUrl chapter only with localized abbreviation`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV", localizedAbbreviation = "NVI")
        val reference = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/GEN.1.NVI", url)
    }

    @Test
    fun `test shareUrl chapter only no abbreviation`() {
        val version = createBibleVersion(id = 999)
        val reference = BibleReference(versionId = 999, bookUSFM = "PSA", chapter = 23)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/999/PSA.23.999", url)
    }

    // ----- Version ID Tests

    @Test
    fun `test shareUrl different version ids`() {
        val version1 = createBibleVersion(id = 1, abbreviation = "KJV")
        val version2 = createBibleVersion(id = 111, abbreviation = "NIV")
        val version3 = createBibleVersion(id = 999, abbreviation = "ESV")

        val reference = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verse = 1)

        val url1 = version1.shareUrl(reference)
        val url2 = version2.shareUrl(reference)
        val url3 = version3.shareUrl(reference)

        assertEquals("https://www.bible.com/bible/1/GEN.1.1.KJV", url1)
        assertEquals("https://www.bible.com/bible/111/GEN.1.1.NIV", url2)
        assertEquals("https://www.bible.com/bible/999/GEN.1.1.ESV", url3)
    }

    // ----- Book USFM Tests

    @Test
    fun `test shareUrl different book usfms`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV")

        val ref1 = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verse = 1)
        val ref2 = BibleReference(versionId = 111, bookUSFM = "EXO", chapter = 2, verse = 3)
        val ref3 = BibleReference(versionId = 111, bookUSFM = "PSA", chapter = 23, verse = 1)
        val ref4 = BibleReference(versionId = 111, bookUSFM = "1SA", chapter = 3, verse = 10)

        val url1 = version.shareUrl(ref1)
        val url2 = version.shareUrl(ref2)
        val url3 = version.shareUrl(ref3)
        val url4 = version.shareUrl(ref4)

        assertEquals("https://www.bible.com/bible/111/GEN.1.1.NIV", url1)
        assertEquals("https://www.bible.com/bible/111/EXO.2.3.NIV", url2)
        assertEquals("https://www.bible.com/bible/111/PSA.23.1.NIV", url3)
        assertEquals("https://www.bible.com/bible/111/1SA.3.10.NIV", url4)
    }

    // ----- Chapter Number Tests

    @Test
    fun `test shareUrl different chapter numbers`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV")
        val reference = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 50, verse = 20)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/GEN.50.20.NIV", url)
    }

    // ----- Edge Cases and Potential Bugs

    @Test
    fun `test shareUrl verse start nil but verse end not nil`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV")

        // Create a reference with verseStart nil but verseEnd not nil
        // Since the initializers don't allow this, we'll test the logic path with chapter only
        val reference = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/GEN.1.NIV", url)
    }

    @Test
    fun `test shareUrl large verse numbers`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV")
        val reference = BibleReference(versionId = 111, bookUSFM = "PSA", chapter = 119, verse = 176)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/PSA.119.176.NIV", url)
    }

    @Test
    fun `test shareUrl large verse range`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV")
        val reference = BibleReference(versionId = 111, bookUSFM = "PSA", chapter = 119, verseStart = 1, verseEnd = 176)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/PSA.119.1-176.NIV", url)
    }

    @Test
    fun `test shareUrl empty abbreviation`() {
        val version = createBibleVersion(id = 111, abbreviation = "")
        val reference = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verse = 1)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/GEN.1.1.111", url)
    }

    @Test
    fun `test shareUrl empty localized abbreviation`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV", localizedAbbreviation = "")
        val reference = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verse = 1)

        val url = version.shareUrl(reference)
        assertEquals("https://www.bible.com/bible/111/GEN.1.1.NIV", url)
    }

    // ----- URL Validity Tests

    @Test
    fun `test shareUrl returns valid url`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV")
        val reference = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verse = 1)

        val url = version.shareUrl(reference)

        assert(url != null)
        assert(url?.startsWith("https://") == true)
        assert(url?.contains("www.bible.com") == true)
        assert(url?.contains("/bible/111/") == true)
    }

    @Test
    fun `test shareUrl all scenarios return valid urls`() {
        val version = createBibleVersion(id = 111, abbreviation = "NIV", localizedAbbreviation = "NVI")

        val scenarios =
            listOf(
                BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1),
                BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verse = 1),
                BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 5),
                BibleReference(versionId = 111, bookUSFM = "PSA", chapter = 23, verse = 1),
                BibleReference(versionId = 111, bookUSFM = "PSA", chapter = 23, verseStart = 1, verseEnd = 6),
                BibleReference(versionId = 111, bookUSFM = "1SA", chapter = 3, verse = 10),
                BibleReference(versionId = 111, bookUSFM = "1SA", chapter = 3, verseStart = 10, verseEnd = 15),
            )

        for (reference in scenarios) {
            val url = version.shareUrl(reference)
            assert(url != null) { "URL should not be null for reference: $reference" }
            assert(url?.startsWith("https://") == true) { "URL should use https scheme for reference: $reference" }
            assert(url?.contains("www.bible.com") == true) { "URL should have correct host for reference: $reference" }
        }
    }
}
