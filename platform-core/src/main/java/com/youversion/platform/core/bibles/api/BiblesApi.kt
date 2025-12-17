package com.youversion.platform.core.bibles.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BiblePassage
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.bibles.models.BibleVersionIndex

interface BiblesApi {
    /**
     * Retrieves a list of [BibleVersion]s available for a specified [languageCode].
     *
     * This function fetches [BibleVersion]s for the provided three-letter language code (e.g., "eng").
     * A valid [com.youversion.platform.core.YouVersionPlatformConfiguration.appKey]  must have been set.
     *
     * @param languageCode An optional letter language code for filtering available Bible versions. If `null` is given,
     *  then expect all versions. If invalid, then an empty list will be returned.
     * @param pageSize informs the Api to return a given number of results per page. Must be > 1 and < 100
     * @return A list of [BibleVersion]s  available for the language.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun versions(
        languageCode: String? = null,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): PaginatedResponse<BibleVersion>

    /**
     * Retrieves a specific [BibleVersion] from the server identified by [versionId].
     *
     * A valid [com.youversion.platform.core.YouVersionPlatformConfiguration.appKey]  must have been set.
     *
     * @param versionId The identifier of the [BibleVersion] to fetch.
     * @return The [BibleVersion] matching the [versionId]
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun version(versionId: Int): BibleVersion

    /**
     *
     */
    suspend fun versionMetaData(versionId: Int): BibleVersion

    /**
     *
     */
    suspend fun versionIndex(versionId: Int): BibleVersionIndex

    /**
     * Retrieves a list of [BibleBook]s for a specific [BibleVersion] identified by [versionId]
     *
     * A valid [com.youversion.platform.core.YouVersionPlatformConfiguration.appKey]  must have been set.
     *
     * @param versionId The identifier of the [BibleVersion] to fetch [BibleBook]s for.
     * @return The list of [BibleBook]s belonging to the [BibleVersion].
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun books(versionId: Int): List<BibleBook>

    /**
     *
     */
    suspend fun book(
        versionId: Int,
        bookUsfm: String,
    ): BibleBook

    /**
     *
     */
    suspend fun chapters(
        versionId: Int,
        bookUsfm: String,
    ): List<BibleChapter>

    /**
     *
     */
    suspend fun chapter(
        versionId: Int,
        bookUsfm: String,
        chapterId: String,
    ): BibleChapter

    /**
     *
     */
    suspend fun verses(
        versionId: Int,
        bookUsfm: String,
        chapterId: String,
    ): List<BibleVerse>

    /**
     *
     */
    suspend fun verse(
        versionId: Int,
        bookUsfm: String,
        chapterId: String,
        verseId: String,
    ): BibleVerse

    /**
     *
     */
    suspend fun passage(
        reference: BibleReference,
        format: String = "html",
    ): BiblePassage
}
