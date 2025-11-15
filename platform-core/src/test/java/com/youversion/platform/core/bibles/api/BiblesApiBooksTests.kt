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

class BiblesApiBooksTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    // ----- Books
    @Test
    fun `test books success returns decoded books`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(request.url.encodedPath, "/v1/bibles/206/books")
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "GEN",
                                "title": "Genesis",
                                "abbreviation": "Gen",
                                "canon": "ot",
                                "chapters": [
                                    "GEN.1",
                                    "GEN.2",
                                ]
                            },
                            {
                                "id": "EXO",
                                "title": "Exodus",
                                "abbreviation": "Exo",
                                "canon": "ot",
                                "chapters": [
                                    "EXO.1",
                                    "EXO.2",
                                ]
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .books(versionId = 206)
                .apply {
                    assertEquals("GEN", this[0].usfm)
                    assertEquals("Genesis", this[0].title)
                    assertEquals("Gen", this[0].abbreviation)
                    assertEquals("ot", this[0].canon)
                    assertEquals("EXO", this[1].usfm)
                }
        }

    @Test
    fun `test books throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.bible.books(206)
        }

    @Test
    fun `test books throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.bible.books(206)
        }

    @Test
    fun `test books throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.bible.books(206)
        }

    @Test
    fun `test books throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.bible.books(206)
        }

    // ----- Book
    @Test
    fun `test book success returns decoded book`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(request.url.encodedPath, "/v1/bibles/206/books/JHN")
                respondJson(
                    """
                    {
                        "id": "GEN",
                        "title": "Genesis",
                        "abbreviation": "Gen",
                        "canon": "ot",
                        "chapters": [
                            "GEN.1",
                            "GEN.2",
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .book(versionId = 206, bookUsfm = "JHN")
                .apply {
                    assertEquals("GEN", usfm)
                    assertEquals("Genesis", title)
                    assertEquals("Gen", abbreviation)
                    assertEquals("ot", canon)
                }
        }

    @Test
    fun `test chapter throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.bible.book(206, "JHN")
        }

    @Test
    fun `test chapter throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.bible.book(206, "JHN")
        }

    @Test
    fun `test chapter throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.bible.book(206, "JHN")
        }

    @Test
    fun `test chapter throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.bible.book(206, "JHN")
        }
}
