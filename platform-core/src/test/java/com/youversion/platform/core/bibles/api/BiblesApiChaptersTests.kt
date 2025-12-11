package com.youversion.platform.core.bibles.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
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
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BiblesApiChaptersTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    // ----- Chapters
    @Test
    fun `test chapters success returns decoded chapters`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(request.url.encodedPath, "/v1/bibles/206/books/JHN/chapters")
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "1",
                                "passage_id": "GEN.1",
                                "title": "1",
                                "verses": [
                                    {
                                        "id": "1",
                                        "passage_id": "GEN.1.1",
                                        "title": "1"
                                    }
                                ]
                            },
                            {
                                "id": "2",
                                "passage_id": "GEN.2",
                                "title": "2",
                                "verses": [
                                    {
                                        "id": "2",
                                        "passage_id": "GEN.2.1",
                                        "title": "2"
                                    }
                                ]
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .chapters(versionId = 206, bookUsfm = "JHN")
                .apply {
                    assertEquals("1", this[0].id)
                    assertEquals("GEN.1", this[0].passageId)
                    assertEquals("1", this[0].title)
                    assertEquals("2", this[1].id)
                }
        }

    @Test
    fun `test chapters throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.bible.chapters(206, "JHN")
        }

    @Test
    fun `test chapters throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.bible.chapters(206, "JHN")
        }

    @Test
    fun `test chapters throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.bible.chapters(206, "JHN")
        }

    @Test
    fun `test chapters throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.bible.chapters(206, "JHN")
        }

    // ----- Chapter
    @Test
    fun `test chapter success returns decoded chapter`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(request.url.encodedPath, "/v1/bibles/206/books/JHN/chapters/3")
                respondJson(
                    """
                    {
                        "id": "1",
                        "passage_id": "GEN.1",
                        "title": "1",
                        "verses": [
                            {
                                "id": "1",
                                "passage_id": "GEN.1.1",
                                "title": "1"
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .chapter(versionId = 206, bookUsfm = "JHN", chapterId = "3")
                .apply { assertEquals("1", id) }
        }

    @Test
    fun `test chapter throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.bible.chapter(206, "JHN", "3")
        }

    @Test
    fun `test chapter throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.bible.chapter(206, "JHN", "3")
        }

    @Test
    fun `test chapter throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.bible.chapter(206, "JHN", "3")
        }

    @Test
    fun `test chapter throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.bible.chapter(206, "JHN", "3")
        }
}
