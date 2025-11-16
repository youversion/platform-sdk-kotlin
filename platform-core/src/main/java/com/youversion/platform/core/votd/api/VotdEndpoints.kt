package com.youversion.platform.core.votd.api

import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.parseApiBody
import com.youversion.platform.core.api.parseApiResponse
import com.youversion.platform.core.utilities.koin.YouVersionPlatformComponent
import com.youversion.platform.core.votd.models.YouVersionVerseOfTheDay
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import io.ktor.http.path

object VotdEndpoints : VotdApi {
    private val httpClient: HttpClient
        get() = YouVersionPlatformComponent.httpClient

    fun votdUrl(dayOfTheYear: Int? = null): String =
        buildYouVersionUrlString {
            path("/v1/verse_of_the_days")
            dayOfTheYear?.let { appendPathSegments(it.toString()) }
        }

    override suspend fun verseOfTheDay(dayOfTheYear: Int): YouVersionVerseOfTheDay =
        httpClient
            .get(votdUrl(dayOfTheYear))
            .let { parseApiBody(it) }

    override suspend fun verseOfTheDays(): List<YouVersionVerseOfTheDay> =
        httpClient
            .get(votdUrl())
            .let { parseApiResponse(it) }
}
