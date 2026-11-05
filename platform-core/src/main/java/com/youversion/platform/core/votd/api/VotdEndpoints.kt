package com.youversion.platform.core.votd.api

import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.languageRanges
import com.youversion.platform.core.api.parseApiBody
import com.youversion.platform.core.api.parseApiResponse
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import com.youversion.platform.core.votd.models.YouVersionVerseOfTheDay
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import io.ktor.http.path

internal object VotdEndpoints : VotdApi {
    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

    /** The VOTD URL for [dayOfTheYear], or the full year when it is `null`. An empty [languageRanges] is omitted. */
    fun votdUrl(
        dayOfTheYear: Int? = null,
        languageRanges: List<String> = emptyList(),
    ): String =
        buildYouVersionUrlString {
            path("/v1/verse-of-the-days")
            dayOfTheYear?.let { appendPathSegments(it.toString()) }
            languageRanges(languageRanges)
        }

    override suspend fun verseOfTheDay(dayOfTheYear: Int): YouVersionVerseOfTheDay =
        verseOfTheDay(dayOfTheYear, emptyList())

    override suspend fun verseOfTheDay(
        dayOfTheYear: Int,
        languageRanges: List<String>,
    ): YouVersionVerseOfTheDay =
        httpClient
            .get(votdUrl(dayOfTheYear, languageRanges))
            .let { parseApiBody(it) }

    override suspend fun verseOfTheDays(): List<YouVersionVerseOfTheDay> = verseOfTheDays(emptyList())

    override suspend fun verseOfTheDays(languageRanges: List<String>): List<YouVersionVerseOfTheDay> =
        httpClient
            .get(votdUrl(languageRanges = languageRanges))
            .let { parseApiResponse(it) }
}
