package com.youversion.platform.core.bibles.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.fields
import com.youversion.platform.core.api.pageSize
import com.youversion.platform.core.api.pageToken
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parseApiBody
import com.youversion.platform.core.api.parseApiResponse
import com.youversion.platform.core.api.parsePaginatedResponse
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BiblePassage
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.bibles.models.BibleVersionIndex
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.path
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object BiblesEndpoints : BiblesApi {
    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

    // ----- Bibles URLs
    fun versionsUrl(
        languageRanges: Set<String> = emptySet(),
        fields: List<String>? = null,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): String =
        buildYouVersionUrlString {
            path("/v1/bibles")
            val ranges = if (languageRanges.isEmpty()) "*" else languageRanges.joinToString(",")
            parameter("language_ranges[]", ranges)
            fields(fields)
            pageSize(pageSize, fields)
            pageToken(pageToken)
        }

    fun versionUrl(versionId: Int): String = buildYouVersionUrlString { path("/v1/bibles/$versionId") }

    fun versionIndexUrl(versionId: Int): String = buildYouVersionUrlString { path("/v1/bibles/$versionId/index") }

    fun versionBooksUrl(versionId: Int): String = buildYouVersionUrlString { path("/v1/bibles/$versionId/books") }

    fun versionBookUrl(
        versionId: Int,
        book: String,
    ): String = buildYouVersionUrlString { path("/v1/bibles/$versionId/books/$book") }

    fun versionBookChaptersUrl(
        versionId: Int,
        book: String,
    ): String = buildYouVersionUrlString { path("/v1/bibles/$versionId/books/$book/chapters") }

    fun versionBookChapterUrl(
        versionId: Int,
        book: String,
        chapterId: String,
    ): String = buildYouVersionUrlString { path("/v1/bibles/$versionId/books/$book/chapters/$chapterId") }

    fun versionBookChapterVersesUrl(
        versionId: Int,
        book: String,
        chapterId: String,
    ): String = buildYouVersionUrlString { path("/v1/bibles/$versionId/books/$book/chapters/$chapterId/verses") }

    fun versionBookChapterVerseUrl(
        versionId: Int,
        book: String,
        chapterId: String,
        verseId: String,
    ): String =
        buildYouVersionUrlString { path("/v1/bibles/$versionId/books/$book/chapters/$chapterId/verses/$verseId") }

    fun passageUrl(
        reference: BibleReference,
        format: String = "html",
    ): String =
        buildYouVersionUrlString {
            path("/v1/bibles/${reference.versionId}/passages/${reference.asUSFM}")
            parameter("format", format)
            parameter("include_notes", true)
            parameter("include_headings", true)
        }

    // ----- Bibles API
    override suspend fun versions(
        languageCode: String?,
        fields: List<String>?,
        pageSize: Int?,
        pageToken: String?,
    ): PaginatedResponse<BibleVersion> {
        val range = languageCode?.let { setOf(it) } ?: emptySet()
        return httpClient
            .get(
                versionsUrl(
                    languageRanges = range,
                    fields = fields,
                    pageSize = pageSize,
                    pageToken = pageToken,
                ),
            ).let {
                when (it.status) {
                    HttpStatusCode.NoContent -> PaginatedResponse(emptyList())
                    else -> parsePaginatedResponse(it)
                }
            }
    }

    override suspend fun version(versionId: Int): BibleVersion =
        coroutineScope {
            val deferredBibleVersion = async { versionMetaData(versionId) }
            val deferredBibleIndex = async { versionIndex(versionId) }

            val basic = deferredBibleVersion.await()
            val index = deferredBibleIndex.await()

            BibleVersion.Builder.merge(basic, index)
        }

    override suspend fun versionMetaData(versionId: Int): BibleVersion =
        httpClient
            .get(versionUrl(versionId))
            .let { parseApiBody(it) }

    override suspend fun versionIndex(versionId: Int): BibleVersionIndex =
        httpClient
            .get(versionIndexUrl(versionId))
            .let { parseApiBody(it) }

    override suspend fun books(versionId: Int): List<BibleBook> =
        httpClient
            .get(versionBooksUrl(versionId))
            .let { parseApiResponse(it) }

    override suspend fun book(
        versionId: Int,
        bookUsfm: String,
    ): BibleBook =
        httpClient
            .get(versionBookUrl(versionId, bookUsfm))
            .let { parseApiBody(it) }

    override suspend fun chapters(
        versionId: Int,
        bookUsfm: String,
    ): List<BibleChapter> =
        httpClient
            .get(versionBookChaptersUrl(versionId, bookUsfm))
            .let { parseApiResponse(it) }

    override suspend fun chapter(
        versionId: Int,
        bookUsfm: String,
        chapterId: String,
    ): BibleChapter =
        httpClient
            .get(versionBookChapterUrl(versionId, bookUsfm, chapterId))
            .let { parseApiBody(it) }

    override suspend fun verses(
        versionId: Int,
        bookUsfm: String,
        chapterId: String,
    ): List<BibleVerse> =
        httpClient
            .get(versionBookChapterVersesUrl(versionId, bookUsfm, chapterId))
            .let { parseApiResponse(it) }

    override suspend fun verse(
        versionId: Int,
        bookUsfm: String,
        chapterId: String,
        verseId: String,
    ): BibleVerse =
        httpClient
            .get(versionBookChapterVerseUrl(versionId, bookUsfm, chapterId, verseId))
            .let { parseApiBody(it) }

    override suspend fun passage(
        reference: BibleReference,
        format: String,
    ): BiblePassage =
        httpClient
            .get(passageUrl(reference, format))
            .let { parseApiBody(it) }
}
