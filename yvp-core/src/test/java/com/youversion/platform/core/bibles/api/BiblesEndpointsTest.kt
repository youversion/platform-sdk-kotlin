package com.youversion.platform.core.bibles.api

import com.youversion.platform.core.bibles.domain.BibleReference
import kotlin.test.Test
import kotlin.test.assertEquals

class BiblesEndpointsTest {
    @Test
    fun `test versionsUrl`() {
        assertEquals(
            "https://api-staging.youversion.com/v1/bibles?language_ranges=%2A",
            BiblesEndpoints.versionsUrl().toString(),
        )

        assertEquals(
            "https://api-staging.youversion.com/v1/bibles?language_ranges=eng",
            BiblesEndpoints.versionsUrl(languageRanges = setOf("eng")).toString(),
        )
    }

    @Test
    fun `test versionUrl`() {
        assertEquals(
            "https://api-staging.youversion.com/v1/bibles/1",
            BiblesEndpoints.versionUrl(versionId = 1).toString(),
        )
    }

    @Test
    fun `test versionIndexUrl`() {
        assertEquals(
            "https://api-staging.youversion.com/v1/bibles/1/index",
            BiblesEndpoints.versionIndexUrl(versionId = 1).toString(),
        )
    }

    @Test
    fun `test versionBooksUrl`() {
        assertEquals(
            "https://api-staging.youversion.com/v1/bibles/1/books",
            BiblesEndpoints.versionBooksUrl(versionId = 1).toString(),
        )
    }

    @Test
    fun `test versionBookUrl`() {
        assertEquals(
            "https://api-staging.youversion.com/v1/bibles/1/books/GEN",
            BiblesEndpoints.versionBookUrl(versionId = 1, book = "GEN").toString(),
        )
    }

    @Test
    fun `test versionBookChaptersUrl`() {
        assertEquals(
            "https://api-staging.youversion.com/v1/bibles/1/books/GEN/chapters",
            BiblesEndpoints.versionBookChaptersUrl(versionId = 1, book = "GEN").toString(),
        )
    }

    @Test
    fun `test versionBookChapterUrl`() {
        assertEquals(
            "https://api-staging.youversion.com/v1/bibles/1/books/GEN/chapters/1",
            BiblesEndpoints.versionBookChapterUrl(versionId = 1, book = "GEN", chapterId = "1").toString(),
        )
    }

    @Test
    fun `test versionBookChapterVersesUrl`() {
        assertEquals(
            "https://api-staging.youversion.com/v1/bibles/1/books/GEN/chapters/1/verses",
            BiblesEndpoints.versionBookChapterVersesUrl(versionId = 1, book = "GEN", chapterId = "1").toString(),
        )
    }

    @Test
    fun `test versionBookChapterVerseUrl`() {
        assertEquals(
            "https://api-staging.youversion.com/v1/bibles/1/books/GEN/chapters/1/verses/1",
            BiblesEndpoints
                .versionBookChapterVerseUrl(
                    versionId = 1,
                    book = "GEN",
                    chapterId = "1",
                    verseId = "1",
                ).toString(),
        )
    }

    @Test
    fun `test passageUrl`() {
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        assertEquals(
            "https://api-staging.youversion.com/v1/bibles/1/passages/GEN.1?format=html&include_notes=true&include_headings=true",
            BiblesEndpoints.passageUrl(reference = reference).toString(),
        )

        assertEquals(
            "https://api-staging.youversion.com/v1/bibles/1/passages/GEN.1?format=json&include_notes=true&include_headings=true",
            BiblesEndpoints.passageUrl(reference = reference, format = "json").toString(),
        )
    }
}
