package com.youversion.platform.core.verseimages.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.verseimages.models.VerseImage
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import com.youversion.platform.helpers.testCannotDownload
import com.youversion.platform.helpers.testForbiddenNotPermitted
import com.youversion.platform.helpers.testInvalidResponse
import com.youversion.platform.helpers.testUnauthorizedNotPermitted
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class VerseImagesApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `test verse images success returns data and paging`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(
                    "/v1/images?passage_id=JHN.3.16&language_ranges%5B%5D=en&page_size=1",
                    request.url.encodedPathAndQuery,
                )
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "abc123",
                                "image_url": "https://images.example.com/abc123",
                                "passage_id": "JHN.3.16",
                                "attribution": "YouVersion",
                                "language_tag": "en",
                                "width": 1280,
                                "height": 720,
                                "aspect_ratio": 1.778
                            }
                        ],
                        "total_size": 4,
                        "next_page_token": "next-token"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val response =
                YouVersionApi.verseImages.verseImages(
                    passageId = "JHN.3.16",
                    languageRanges = listOf("en"),
                    pageSize = 1,
                )

            assertEquals(
                listOf(
                    VerseImage(
                        id = "abc123",
                        imageUrl = "https://images.example.com/abc123",
                        passageId = "JHN.3.16",
                        attribution = "YouVersion",
                        languageTag = "en",
                        width = 1280,
                        height = 720,
                        aspectRatio = 1.778,
                    ),
                ),
                response.data,
            )
            assertEquals(4, response.totalSize)
            assertEquals("next-token", response.nextPageToken)
        }

    @Test
    fun `test verse images returns an empty page when nothing matches`() =
        runTest {
            MockEngine { respondJson("""{"data": []}""") }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val response = YouVersionApi.verseImages.verseImages(passageId = "JHN.3.16")

            assertTrue(response.data.isEmpty())
            assertNull(response.totalSize)
            assertNull(response.nextPageToken)
        }

    @Test
    fun `test verse image success returns the image`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/v1/images/abc123", request.url.encodedPath)
                respondJson(
                    """
                    {
                        "id": "abc123",
                        "image_url": "https://images.example.com/abc123",
                        "passage_id": "1CO.13.4-7",
                        "attribution": "YouVersion",
                        "language_tag": "es",
                        "width": 1080,
                        "height": 1080,
                        "aspect_ratio": 1.0
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val image = YouVersionApi.verseImages.verseImage(imageId = "abc123")

            assertEquals(
                VerseImage(
                    id = "abc123",
                    imageUrl = "https://images.example.com/abc123",
                    passageId = "1CO.13.4-7",
                    attribution = "YouVersion",
                    languageTag = "es",
                    width = 1080,
                    height = 1080,
                    aspectRatio = 1.0,
                ),
                image,
            )
        }

    @Test
    fun `test verse images rejects a page size outside one to ninety nine`() =
        runTest {
            startNoRequestExpected()
            listOf(0, 100).forEach { pageSize ->
                assertFailsWith<IllegalArgumentException>("expected $pageSize to be rejected") {
                    YouVersionApi.verseImages.verseImages(pageSize = pageSize)
                }
            }
        }

    @Test
    fun `test verse image rejects an empty image id`() =
        runTest {
            startNoRequestExpected()
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.verseImages.verseImage(imageId = "")
            }
        }

    @Test
    fun `test verse images throws not permitted when unauthorized`() =
        testUnauthorizedNotPermitted { YouVersionApi.verseImages.verseImages() }

    @Test
    fun `test verse images throws not permitted when forbidden`() =
        testForbiddenNotPermitted { YouVersionApi.verseImages.verseImages() }

    @Test
    fun `test verse images throws cannot download on server error`() =
        testCannotDownload { YouVersionApi.verseImages.verseImages() }

    @Test
    fun `test verse images throws invalid response on malformed body`() =
        testInvalidResponse { YouVersionApi.verseImages.verseImages() }

    @Test
    fun `test verse image throws not permitted when unauthorized`() =
        testUnauthorizedNotPermitted { YouVersionApi.verseImages.verseImage(imageId = "abc123") }

    @Test
    fun `test verse image throws not permitted when forbidden`() =
        testForbiddenNotPermitted { YouVersionApi.verseImages.verseImage(imageId = "abc123") }

    @Test
    fun `test verse image throws cannot download on server error`() =
        testCannotDownload { YouVersionApi.verseImages.verseImage(imageId = "abc123") }

    @Test
    fun `test verse image throws invalid response on malformed body`() =
        testInvalidResponse { YouVersionApi.verseImages.verseImage(imageId = "abc123") }

    private fun startNoRequestExpected() {
        MockEngine { fail("no request should have been issued") }
            .also { engine -> startYouVersionPlatformTest(engine) }
        YouVersionPlatformConfiguration.configure(appKey = "app")
    }
}
