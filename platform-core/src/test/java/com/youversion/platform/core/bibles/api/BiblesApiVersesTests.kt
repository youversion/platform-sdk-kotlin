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

class BiblesApiVersesTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    // ----- Verses
    @Test
    fun `test verses success returns decoded verses`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(request.url.encodedPath, "/v1/bibles/206/books/JHN/chapters/3/verses")
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "1",
                                "reference": "Genesis 1:1",
                                "book_id": "GEN",
                                "chapter_id": "1",
                                "passage_id": "GEN.1.1"
                            },
                            {
                                "id": "2",
                                "reference": "Genesis 1:2",
                                "book_id": "GEN",
                                "chapter_id": "1",
                                "passage_id": "GEN.1.2"
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .verses(versionId = 206, bookUsfm = "JHN", chapterId = "3")
                .apply {
                    assertEquals("1", this[0].id)
                    assertEquals("2", this[1].id)
                }
        }

    @Test
    fun `test verses throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.bible.verses(206, "JHN", "3")
        }

    @Test
    fun `test verses throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.bible.verses(206, "JHN", "3")
        }

    @Test
    fun `test verses throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.bible.verses(206, "JHN", "3")
        }

    @Test
    fun `test verses throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.bible.verses(206, "JHN", "3")
        }

    // ----- Verse
    @Test
    fun `test verse success returns decoded verse`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(request.url.encodedPath, "/v1/bibles/206/books/JHN/chapters/3/verses/1")
                respondJson(
                    """
                    {
                        "id": "1",
                        "reference": "Genesis 1:1",
                        "book_id": "GEN",
                        "chapter_id": "1",
                        "passage_id": "GEN.1.1"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .verse(versionId = 206, bookUsfm = "JHN", chapterId = "3", verseId = "1")
                .apply { assertEquals("1", id) }
        }

    @Test
    fun `test verse throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.bible.verse(206, "JHN", "3", "1")
        }

    @Test
    fun `test verse throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.bible.verse(206, "JHN", "3", "1")
        }

    @Test
    fun `test verse throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.bible.verse(206, "JHN", "3", "1")
        }

    @Test
    fun `test verse throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.bible.verse(206, "JHN", "3", "1")
        }
}
