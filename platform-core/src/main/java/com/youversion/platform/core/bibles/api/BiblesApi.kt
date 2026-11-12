package com.youversion.platform.core.bibles.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BiblePassage
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.bibles.models.BibleVersionIndex

public interface BiblesApi {
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
    public suspend fun versions(
        languageCode: String? = null,
        fields: List<String>? = null,
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
    public suspend fun version(versionId: Int): BibleVersion

    /**
     *
     */
    public suspend fun versionMetaData(versionId: Int): BibleVersion

    /**
     *
     */
    public suspend fun versionIndex(versionId: Int): BibleVersionIndex

    /**
     * Retrieves a list of [BibleBook]s for a specific [BibleVersion] identified by [versionId]
     *
     * A valid [com.youversion.platform.core.YouVersionPlatformConfiguration.appKey]  must have been set.
     *
     * @param versionId The identifier of the [BibleVersion] to fetch [BibleBook]s for.
     * @return The list of [BibleBook]s belonging to the [BibleVersion].
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    public suspend fun books(versionId: Int): List<BibleBook>

    /**
     *
     */
    public suspend fun book(
        versionId: Int,
        bookUsfm: String,
    ): BibleBook

    /**
     *
     */
    public suspend fun chapters(
        versionId: Int,
        bookUsfm: String,
    ): List<BibleChapter>

    /**
     *
     */
    public suspend fun chapter(
        versionId: Int,
        bookUsfm: String,
        chapterId: String,
    ): BibleChapter

    /**
     *
     */
    public suspend fun verses(
        versionId: Int,
        bookUsfm: String,
        chapterId: String,
    ): List<BibleVerse>

    /**
     *
     */
    public suspend fun verse(
        versionId: Int,
        bookUsfm: String,
        chapterId: String,
        verseId: String,
    ): BibleVerse

    /**
     *
     */
    public suspend fun passage(
        reference: BibleReference,
        format: String = "html",
    ): BiblePassage

    /**
     * Retrieves a [BiblePassage] for a given version and passage ID string.
     *
     * This supports non-standard passages such as intro chapters that cannot
     * be represented by a [BibleReference].
     *
     * @param versionId The identifier of the Bible version.
     * @param passageId The passage ID string (e.g., "GEN.INTRO").
     * @param format The desired format of the passage content.
     * @return The [BiblePassage] for the given passage ID.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    public suspend fun passage(
        versionId: Int,
        passageId: String,
        format: String = "html",
    ): BiblePassage
}
