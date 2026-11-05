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

    /**
     * Retrieves the verse of the day for [dayOfTheYear], with its image chosen for [languageRanges].
     *
     * @param dayOfTheYear Which verse of the day to get, starting at 1.
     * @param languageRanges BCP 47 language ranges, most preferred first, such as `["es-MX", "en"]`, used to pick
     *     the day's image. An empty list sends none and lets the server pick its default.
     * @return The day's [YouVersionVerseOfTheDay], whose [YouVersionVerseOfTheDay.imageId] is `null` when the day
     *     has no image.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun verseOfTheDay(
        dayOfTheYear: Int,
        languageRanges: List<String>,
    ): YouVersionVerseOfTheDay

    /** Retrieves the verse of the day for every day of the year. */
    suspend fun verseOfTheDays(): List<YouVersionVerseOfTheDay>

    /**
     * Retrieves the verse of the day for every day of the year, with images chosen for [languageRanges].
     *
     * @param languageRanges BCP 47 language ranges, most preferred first, used to pick each day's image. An empty
     *     list sends none and lets the server pick its default.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun verseOfTheDays(languageRanges: List<String>): List<YouVersionVerseOfTheDay>
}
