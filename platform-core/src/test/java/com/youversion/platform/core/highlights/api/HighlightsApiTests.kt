package com.youversion.platform.core.highlights.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HighlightsApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `test create highlight success`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)

                val jsonData = request.body.toByteArray().decodeToString()
                val decoded: JsonObject = Json.Default.decodeFromString(jsonData)

                assertEquals(1, decoded["version_id"]?.jsonPrimitive?.int)
                assertEquals("GEN.1.1", decoded["passage_id"]?.jsonPrimitive?.content)
                assertEquals("ff00ff", decoded["color"]?.jsonPrimitive?.content)

                respond("", HttpStatusCode.Companion.Created)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            assertTrue {
                YouVersionApi.highlights.createHighlight(1, "GEN.1.1", "FF00FF")
            }
        }

    @Test
    fun `test get highlights parses response`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Companion.Get, request.method)

                val url = request.url
                assertEquals("1", url.parameters["version_id"])
                assertEquals("GEN.9", url.parameters["passage_id"])

                respondJson(
                    """
                    {
                       "data": [
                          {
                             "id": "1",
                             "version_id": 1,
                             "passage_id": "GEN.9.1",
                             "color": "ff00ff"
                          }
                       ],
                       "next_page_token": null
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app", accessToken = "token")
            val highlights =
                YouVersionApi.highlights.highlights(
                    versionId = 1,
                    passageId = "GEN.9",
                )

            assertEquals(1, highlights.size)
            assertEquals("GEN.9.1", highlights.first().passageId)
        }

    @Test
    fun `test get highlights unauthorized returns empty`() =
        runTest {
            MockEngine { request ->
                respond("", HttpStatusCode.Unauthorized)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app", accessToken = "token")
            assertTrue { YouVersionApi.highlights.highlights(1, "GEN.1").isEmpty() }
        }

    @Test
    fun `test get highlights forbidden returns empty`() =
        runTest {
            MockEngine { request ->
                respond("", HttpStatusCode.Forbidden)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app", accessToken = "token")
            assertTrue { YouVersionApi.highlights.highlights(1, "GEN.1").isEmpty() }
        }

    @Test
    fun `test get highlights failure returns empty`() =
        runTest {
            MockEngine { request ->
                respond("", HttpStatusCode.InternalServerError)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app", accessToken = "token")
            assertTrue { YouVersionApi.highlights.highlights(1, "GEN.1").isEmpty() }
        }

    @Test
    fun `test get highlights non content returns empty`() =
        runTest {
            MockEngine { request ->
                respond("", HttpStatusCode.NoContent)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app", accessToken = "token")
            assertTrue { YouVersionApi.highlights.highlights(1, "GEN.1").isEmpty() }
        }

    @Test
    fun `test delete highlight success`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Companion.Delete, request.method)
                assertEquals("application/json", request.headers["content-type"])

                val url = request.url
                assertEquals("1", url.parameters["version_id"])
                assertTrue(url.encodedPath.contains("GEN.5.7"))

                respond("", HttpStatusCode.Companion.NoContent)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app", accessToken = "token")
            assertTrue {
                YouVersionApi.highlights.deleteHighlight(
                    versionId = 1,
                    passageId = "GEN.5.7",
                )
            }
        }
}
