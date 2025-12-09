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
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BiblesApiVersionsTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    // ----- Versions
    @Test
    fun `test versions success returns decoded version metadata`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(request.url.encodedPath, "/v1/bibles")
                respondJson(VERSIONS_ENG_JSON)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .versions("eng")
                .apply {
                    assertEquals(12, this[0].id)
                }
        }

    @Test
    fun `test versions returns empty list if language code is not 3 letters`() =
        runTest {
            MockEngine { request ->
                respondJson(VERSIONS_ENG_JSON)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .versions("en")
                .apply {
                    assertTrue { isEmpty() }
                }
        }

    @Test
    fun `test versions returns empty list if language code is not a known code`() =
        runTest {
            MockEngine { request ->
                respond("", HttpStatusCode.NoContent)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .versions("foo")
                .apply {
                    assertTrue { isEmpty() }
                }
        }

    @Test
    fun `test versions queries all versions if language code is null`() =
        runTest {
            MockEngine { request ->
                assertEquals("*", request.url.parameters["language_ranges[]"])
                respondJson(VERSIONS_ENG_JSON)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .versions()
                .apply {
                    assertFalse { isEmpty() }
                }
        }

    @Test
    fun `test versions throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.bible.versions("eng")
        }

    @Test
    fun `test versions throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.bible.versions("eng")
        }

    @Test
    fun `test versions throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.bible.versions("eng")
        }

    @Test
    fun `test versions throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.bible.versions("eng")
        }

    // ----- Version
    @Test
    fun `test version success returns combined meta and index data`() =
        runTest {
            MockEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/bibles/206" -> respondJson(VERSION_206_METADATA_JSON)
                    "/v1/bibles/206/index" -> respondJson(VERSION_206_INDEX_JSON)
                    else -> throw IllegalArgumentException("Unexpected request path: ${request.url.encodedPath}")
                }
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .version(versionId = 206)
                .apply {
                    // Assert metadata fields from bible_206.json
                    assertEquals(206, id)
                    assertEquals("engWEBUS", abbreviation)
                    assertEquals("This Public Domain Bible text is courtesy of eBible.org.", copyrightLong)
                    assertEquals("PUBLIC DOMAIN (not copyrighted)", copyrightShort)
                    assertEquals("en", languageTag)
                    assertEquals("WEBUS", localizedAbbreviation)
                    assertEquals(
                        "World English Bible, American English Edition, without Strong's Numbers",
                        localizedTitle,
                    )
                    assertEquals("World English Bible, American English Edition, without Strong's Numbers", title)
                    assertNull(readerFooter)
                    assertNull(readerFooterUrl)

                    // Assert text direction from index
                    assertEquals("ltr", textDirection)

                    // Assert bookCodes
                    assertEquals(listOf("GEN", "EXO", "LEV"), bookCodes)

                    // Assert books structure
                    assertEquals(3, books?.size)

                    // Assert Genesis book details
                    books?.get(0)?.apply {
                        assertEquals("GEN", usfm)
                        assertEquals("Genesis", title)
                        assertEquals("Gen", abbreviation)
                        assertEquals("old_testament", canon)

                        // Assert chapters
                        assertEquals(4, chapters?.size)

                        // Assert GEN.1 chapter
                        chapters?.get(0)?.apply {
                            assertEquals("1", id)
                            assertEquals(true, isCanonical)
                            assertEquals("GEN.1", passageId)
                            assertEquals("1", title)
                        }

                        // Assert GEN.2 chapter
                        chapters?.get(1)?.apply {
                            assertEquals("2", id)
                            assertEquals(true, isCanonical)
                            assertEquals("GEN.2", passageId)
                            assertEquals("2", title)
                        }
                    }
                }
        }

    @Test
    fun `test version throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.bible.versionMetaData(206)
        }

    @Test
    fun `test version throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.bible.version(206)
        }

    @Test
    fun `test version throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.bible.version(206)
        }

    @Test
    fun `test version throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.bible.version(206)
        }

    // ----- Version Metadata
    @Test
    fun `test versionMetaData success returns decoded version metadata`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(request.url.encodedPath, "/v1/bibles/206")
                respondJson(VERSION_206_METADATA_JSON)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .versionMetaData(versionId = 206)
                .apply {
                    assertEquals(206, id)
                    assertEquals("engWEBUS", abbreviation)
                    assertEquals("PUBLIC DOMAIN (not copyrighted)", copyrightShort)
                    assertEquals("World English Bible, American English Edition, without Strong's Numbers", title)
                    assertNull(books)
                }
        }

    @Test
    fun `test versionMetaData throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.bible.versionMetaData(206)
        }

    @Test
    fun `test versionMetaData throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.bible.versionMetaData(206)
        }

    @Test
    fun `test versionMetaData throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.bible.versionMetaData(206)
        }

    @Test
    fun `test versionMetaData throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.bible.versionMetaData(206)
        }

    // ----- Version Index
    @Test
    fun `test versionIndex success returns decoded index`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(request.url.encodedPath, "/v1/bibles/206/index")
                respondJson(VERSION_206_INDEX_JSON)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.bible
                .versionIndex(versionId = 206)
                .apply {
                    assertEquals("ltr", textDirection)
                    assertEquals("GEN", books?.get(0)?.id)
                }
        }

    @Test
    fun `test versionIndex throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.bible.versionIndex(206)
        }

    @Test
    fun `test versionIndex throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.bible.versionIndex(206)
        }

    @Test
    fun `test versionIndex throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.bible.versionIndex(206)
        }

    @Test
    fun `test versionIndex throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.bible.versionIndex(206)
        }
}

private const val VERSIONS_ENG_JSON = """
{
    "data": [
        {
            "id": 12,
            "abbreviation": "ASV",
            "copyright_long": null,
            "copyright_short": null,
            "info": null,
            "publisher_url": null,
            "language_tag": "en",
            "local_abbreviation": "ASV",
            "local_title": "American Standard Version",
            "title": "American Standard Version",
            "books": [
                "GEN",
                "EXO",
                "LEV"
            ]
        }
    ]
}                 
"""
private const val VERSION_206_METADATA_JSON = """
{
  "id": 206,
  "abbreviation": "engWEBUS",
  "copyright_long": "This Public Domain Bible text is courtesy of eBible.org.",
  "copyright_short": "PUBLIC DOMAIN (not copyrighted)",
  "info": null,
  "publisher_url": null,
  "language_tag": "en",
  "localized_abbreviation": "WEBUS",
  "localized_title": "World English Bible, American English Edition, without Strong's Numbers",
  "title": "World English Bible, American English Edition, without Strong's Numbers",
  "books": [
    "GEN",
    "EXO",
    "LEV"
  ],
  "youversion_deep_link": "https://www.bible.com/versions/206"
}
"""

private const val VERSION_206_INDEX_JSON = """
{
    "text_direction": "ltr",
    "books": [
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
                        },
                        {
                            "id": "2",
                            "passage_id": "GEN.1.2",
                            "title": "2"
                        }
                    ]
                },
                {
                    "id": "2",
                    "passage_id": "GEN.2",
                    "title": "2",
                    "verses": [
                        {
                            "id": "1",
                            "passage_id": "GEN.2.1",
                            "title": "1"
                        },
                        {
                            "id": "2",
                            "passage_id": "GEN.2.2",
                            "title": "2"
                        }
                    ]
                },
                {
                    "id": "3",
                    "passage_id": "GEN.3",
                    "title": "3",
                    "verses": null
                },
                {
                    "id": "Intro",
                    "passage_id": "GEN.intro",
                    "title": "Introduction",
                    "verses": null
                }
            ]
        },
        {
          "id": "EXO",
          "title": "Exodus",
          "full_title": "The Second Book of Moses, Called Exodus",
          "abbreviation": "Exo",
          "canon": "old_testament",
          "chapters": null
        },
        {
          "id": "LEV",
          "title": "Leviticus",
          "full_title": "The Third Book of Moses, Called Leviticus",
          "abbreviation": "Lev",
          "canon": "old_testament",
          "chapters": []
        }
    ]
}
"""
