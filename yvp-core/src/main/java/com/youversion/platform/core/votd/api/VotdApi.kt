package com.youversion.platform.core.votd.api

import com.youversion.platform.core.votd.models.YouVersionVerseOfTheDay

interface VotdApi {
    /**
     * Retrieves the Verse of the Day from YouVersion.
     *
     * A valid [com.youversion.platform.core.YouVersionPlatformConfiguration.appKey] must be set before calling this function.
     *
     * @param dayOfTheYear Which verse of the day to get
     * @return A [com.youversion.platform.core.votd.models.YouVersionVerseOfTheDay] containing the verse text, reference, and associated information.
     * @throws [io.ktor.client.plugins.ClientRequestException] if the server response could not be decoded.
     */
    suspend fun verseOfTheDay(dayOfTheYear: Int = 1): YouVersionVerseOfTheDay

    suspend fun verseOfTheDays(): List<YouVersionVerseOfTheDay>
}
