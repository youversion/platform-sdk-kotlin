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
import kotlin.test.assertTrue

class LanguagesApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `test languages success returns data`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Companion.Get, request.method)
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
            val languages = YouVersionApi.languages.languages()

            assertEquals(2, languages.size)
            assertEquals("en", languages[0].id)
            assertEquals("English", languages[0].language)
            assertEquals(111, languages[0].defaultBibleVersionId)
            assertEquals("es", languages[1].id)
            assertEquals("Spanish", languages[1].language)
            assertEquals(128, languages[1].defaultBibleVersionId)
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
                .apply { assertTrue { isEmpty() } }
        }

    @Test
    fun `test books throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.languages.languages(country = "US")
        }

    @Test
    fun `test books throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.languages.languages(country = "US")
        }

    @Test
    fun `test books throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.languages.languages(country = "US")
        }

    @Test
    fun `test books throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.languages.languages(country = "US")
        }
}
