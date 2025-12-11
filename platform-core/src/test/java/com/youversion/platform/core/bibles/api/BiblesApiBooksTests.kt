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
                                "full_title": "The First Book of Moses, Commonly Called Genesis",
                                "abbreviation": "Gen",
                                "canon": "old_testament",
                                "chapters": [
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
                            },
                            {
                                "id": "EXO",
                                "title": "Exodus",
                                "full_title": "The Second Book of Moses, Commonly Called Exodus",
                                "abbreviation": "Exo",
                                "canon": "old_testament",
                                "chapters": [
                                    {
                                        "id": "1",
                                        "passage_id": "EXO.1",
                                        "title": "1",
                                        "verses": [
                                            {
                                                "id": "1",
                                                "passage_id": "EXO.1.1",
                                                "title": "1"
                                            },
                                            {
                                                "id": "2",
                                                "passage_id": "EXO.1.2",
                                                "title": "2"
                                            }
                                        ]
                                    },
                                    {
                                        "id": "2",
                                        "passage_id": "EXO.2",
                                        "title": "2",
                                        "verses": [
                                            {
                                                "id": "1",
                                                "passage_id": "EXO.2.1",
                                                "title": "1"
                                            },
                                            {
                                                "id": "2",
                                                "passage_id": "EXO.2.2",
                                                "title": "2"
                                            }
                                        ]
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
                .books(versionId = 206)
                .apply {
                    assertEquals("GEN", this[0].usfm)
                    assertEquals("Genesis", this[0].title)
                    assertEquals("The First Book of Moses, Commonly Called Genesis", this[0].fullTitle)
                    assertEquals("Gen", this[0].abbreviation)
                    assertEquals("old_testament", this[0].canon)
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
                        "full_title": "The First Book of Moses, Commonly Called Genesis",
                        "abbreviation": "Gen",
                        "canon": "old_testament",
                        "chapters": [
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
                .book(versionId = 206, bookUsfm = "JHN")
                .apply {
                    assertEquals("GEN", usfm)
                    assertEquals("Genesis", title)
                    assertEquals("The First Book of Moses, Commonly Called Genesis", fullTitle)
                    assertEquals("Gen", abbreviation)
                    assertEquals("old_testament", canon)
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
