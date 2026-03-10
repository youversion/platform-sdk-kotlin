package com.youversion.platform.core.languages.api

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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LanguagesApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `test languages success returns data`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/v1/languages", request.url.encodedPathAndQuery)
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "en",
                                "language": "English",
                                "script": "Latn",
                                "script_name": "Latin",
                                "aliases": ["eng"],
                                "display_names": {"en": "English", "es": "Inglés"},
                                "scripts": ["Latn"],
                                "variants": ["US", "GB"],
                                "countries": ["US", "GB", "CA"],
                                "text_direction": "ltr",
                                "default_bible_version_id": 111
                            },
                            {
                                "id": "es",
                                "language": "Spanish",
                                "script": "Latn",
                                "script_name": "Latin",
                                "aliases": ["spa"],
                                "display_names": {"en": "Spanish", "es": "Español"},
                                "scripts": ["Latn"],
                                "variants": ["ES", "MX"],
                                "countries": ["ES", "MX", "AR"],
                                "text_direction": "ltr",
                                "default_bible_version_id": 128
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val languages = YouVersionApi.languages.languages().data

            assertEquals(2, languages.size)

            val english = languages[0]
            assertEquals("en", english.id)
            assertEquals("English", english.language)
            assertEquals("Latn", english.script)
            assertEquals("Latin", english.scriptName)
            assertEquals(listOf("eng"), english.aliases)
            assertEquals(mapOf("en" to "English", "es" to "Inglés"), english.displayNames)
            assertEquals(listOf("Latn"), english.scripts)
            assertEquals(listOf("US", "GB"), english.variants)
            assertEquals(listOf("US", "GB", "CA"), english.countries)
            assertEquals("ltr", english.textDirection)
            assertEquals(111, english.defaultBibleVersionId)

            val spanish = languages[1]
            assertEquals("es", spanish.id)
            assertEquals("Spanish", spanish.language)
            assertEquals("Latn", spanish.script)
            assertEquals("Latin", spanish.scriptName)
            assertEquals(listOf("spa"), spanish.aliases)
            assertEquals(mapOf("en" to "Spanish", "es" to "Español"), spanish.displayNames)
            assertEquals(listOf("Latn"), spanish.scripts)
            assertEquals(listOf("ES", "MX"), spanish.variants)
            assertEquals(listOf("ES", "MX", "AR"), spanish.countries)
            assertEquals("ltr", spanish.textDirection)
            assertEquals(128, spanish.defaultBibleVersionId)
        }

    @Test
    fun `test languages defaults textDirection to ltr when omitted`() =
        runTest {
            MockEngine { request ->
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "en",
                                "language": "English"
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val languages = YouVersionApi.languages.languages().data

            assertEquals(1, languages.size)
            assertEquals("en", languages[0].id)
            assertEquals("English", languages[0].language)
            assertEquals("ltr", languages[0].textDirection)
            assertNull(languages[0].script)
            assertNull(languages[0].scriptName)
            assertNull(languages[0].aliases)
            assertNull(languages[0].displayNames)
            assertNull(languages[0].scripts)
            assertNull(languages[0].variants)
            assertNull(languages[0].countries)
            assertNull(languages[0].defaultBibleVersionId)
        }

    @Test
    fun `test languages returns empty list if an unknown country is provided`() =
        runTest {
            MockEngine { request ->
                respond("", HttpStatusCode.NoContent)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.languages
                .languages()
                .apply { assertTrue { data.isEmpty() } }
        }

    @Test
    fun `test languages throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.languages.languages(country = "US")
        }

    @Test
    fun `test languages throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.languages.languages(country = "US")
        }

    @Test
    fun `test languages throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.languages.languages(country = "US")
        }

    @Test
    fun `test languages throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.languages.languages(country = "US")
        }
}
