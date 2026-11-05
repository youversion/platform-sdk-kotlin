package com.youversion.platform.core.votd.api

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.votd.models.YouVersionVerseOfTheDay
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import com.youversion.platform.helpers.testCannotDownload
import com.youversion.platform.helpers.testForbiddenNotPermitted
import com.youversion.platform.helpers.testInvalidResponse
import com.youversion.platform.helpers.testUnauthorizedNotPermitted
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VotdApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    // ----- VOTD
    @Test
    fun `test verseOfTheDay success returns decoded verse of the day`() =
        runTest {
            MockEngine { request ->
                assertEquals("/v1/verse-of-the-days/1", request.url.encodedPath)
                assertEquals(
                    emptyList(),
                    request.url.parameters
                        .getAll("language_ranges[]")
                        .orEmpty(),
                )
                respondJson(
                    """
                    {
                        "day": 1,
                        "passage_id": "ISA.43.19"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionApi.votd.verseOfTheDay(1).apply {
                assertEquals(1, day)
                assertEquals("ISA.43.19", passageUsfm)
                assertNull(imageId)
            }
        }

    @Test
    fun `test verseOfTheDay with language ranges sends them and returns the image id`() =
        runTest {
            MockEngine { request ->
                assertEquals("/v1/verse-of-the-days/1", request.url.encodedPath)
                assertEquals(listOf("es-MX", "en"), request.url.parameters.getAll("language_ranges[]"))
                respondJson(
                    """
                    {
                        "day": 1,
                        "passage_id": "ISA.43.18-19",
                        "image_id": "cHJlcmVuZGVyZWQtdmVyc2UtaW1hZ2U6MTE2MzE2"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionApi.votd.verseOfTheDay(dayOfTheYear = 1, languageRanges = listOf("es-MX", "en")).apply {
                assertEquals(1, day)
                assertEquals("ISA.43.18-19", passageUsfm)
                assertEquals("cHJlcmVuZGVyZWQtdmVyc2UtaW1hZ2U6MTE2MzE2", imageId)
            }
        }

    @Test
    fun `test verseOfTheDay returns a null image id when the response sends null`() =
        runTest {
            MockEngine {
                respondJson(
                    """
                    {
                        "day": 1,
                        "passage_id": "ISA.43.19",
                        "image_id": null
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionApi.votd.verseOfTheDay(dayOfTheYear = 1, languageRanges = listOf("en")).apply {
                assertEquals(1, day)
                assertEquals("ISA.43.19", passageUsfm)
                assertNull(imageId)
            }
        }

    @Test
    fun `test verseOfTheDay throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.votd.verseOfTheDay(206)
        }

    @Test
    fun `test verseOfTheDay throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.votd.verseOfTheDay(206)
        }

    @Test
    fun `test verseOfTheDay throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.votd.verseOfTheDay(206)
        }

    @Test
    fun `test verseOfTheDay throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.votd.verseOfTheDay(206)
        }

    // ----- Verse of the Days
    @Test
    fun `test verseOfTheDays success returns decoded list of verse of the day`() =
        runTest {
            MockEngine { request ->
                assertEquals("/v1/verse-of-the-days", request.url.encodedPath)
                assertEquals(
                    emptyList(),
                    request.url.parameters
                        .getAll("language_ranges[]")
                        .orEmpty(),
                )
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "day": 1,
                                "passage_id": "ISA.43.19"
                            },
                            {
                                "day": 2,
                                "passage_id": "HEB.13.5"
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionApi.votd.verseOfTheDays().apply {
                assertEquals(1, get(0).day)
                assertEquals("ISA.43.19", get(0).passageUsfm)
                assertNull(get(0).imageId)

                assertEquals(2, get(1).day)
                assertEquals("HEB.13.5", get(1).passageUsfm)
                assertNull(get(1).imageId)
            }
        }

    @Test
    fun `test verseOfTheDays with language ranges sends them and returns each image id`() =
        runTest {
            MockEngine { request ->
                assertEquals("/v1/verse-of-the-days", request.url.encodedPath)
                assertEquals(listOf("en"), request.url.parameters.getAll("language_ranges[]"))
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "day": 1,
                                "passage_id": "ISA.43.19",
                                "image_id": "aW1hZ2UtMQ"
                            },
                            {
                                "day": 2,
                                "passage_id": "HEB.13.5",
                                "image_id": "aW1hZ2UtMg"
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionApi.votd.verseOfTheDays(languageRanges = listOf("en")).apply {
                assertEquals(YouVersionVerseOfTheDay(1, "ISA.43.19", "aW1hZ2UtMQ"), get(0))
                assertEquals(YouVersionVerseOfTheDay(2, "HEB.13.5", "aW1hZ2UtMg"), get(1))
            }
        }
}
