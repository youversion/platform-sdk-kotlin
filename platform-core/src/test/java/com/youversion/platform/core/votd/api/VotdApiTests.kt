package com.youversion.platform.core.votd.api

import com.youversion.platform.core.api.YouVersionApi
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

class VotdApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    // ----- VOTD
    @Test
    fun `test verseOfTheDay success returns decoded verse of the day`() =
        runTest {
            MockEngine { request ->
                assertEquals("/v1/verse_of_the_days/1", request.url.encodedPath)
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
                assertEquals("/v1/verse_of_the_days", request.url.encodedPath)
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

                assertEquals(2, get(1).day)
                assertEquals("HEB.13.5", get(1).passageUsfm)
            }
        }
}
