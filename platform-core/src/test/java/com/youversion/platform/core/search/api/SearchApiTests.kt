package com.youversion.platform.core.search.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class SearchApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `test suggested queries success returns data`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(
                    "/v1/search-queries?language_ranges%5B%5D=en-US&language_ranges%5B%5D=es&query=love",
                    request.url.encodedPathAndQuery,
                )
                respondJson(
                    """
                    {
                        "data": [
                            { "text": "love", "source": "trending" },
                            { "text": "love your enemies" }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val queries =
                YouVersionApi.search.suggestedQueries(
                    query = "love",
                    languageRanges = listOf("en-US", "es"),
                )

            assertEquals(2, queries.size)
            assertEquals("love", queries[0].text)
            assertEquals("trending", queries[0].source)
            assertEquals("love your enemies", queries[1].text)
            assertNull(queries[1].source)
        }

    @Test
    fun `test trending queries success returns data`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(
                    "/v1/search-queries?language_ranges%5B%5D=%2A&trending=true",
                    request.url.encodedPathAndQuery,
                )
                respondJson(
                    """
                    {
                        "data": [
                            { "text": "peace", "source": "popular" }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val queries = YouVersionApi.search.trendingQueries(languageRanges = listOf("*"))

            assertEquals(1, queries.size)
            assertEquals("peace", queries[0].text)
            assertEquals("popular", queries[0].source)
        }

    @Test
    fun `test suggested queries returns an empty list if there is no content`() =
        runTest {
            MockEngine { respond("", HttpStatusCode.NoContent) }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            assertTrue {
                YouVersionApi.search
                    .suggestedQueries(query = "love", languageRanges = listOf("en"))
                    .isEmpty()
            }
        }

    @Test
    fun `test trending queries returns an empty list if there is no content`() =
        runTest {
            MockEngine { respond("", HttpStatusCode.NoContent) }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            assertTrue { YouVersionApi.search.trendingQueries(languageRanges = listOf("en")).isEmpty() }
        }

    @Test
    fun `test suggested queries rejects an empty query`() =
        runTest {
            startNoRequestExpected()
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.search.suggestedQueries(query = "", languageRanges = listOf("en"))
            }
        }

    @Test
    fun `test suggested queries accepts a query longer than one hundred characters`() =
        runTest {
            MockEngine { respond("", HttpStatusCode.NoContent) }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            assertTrue {
                YouVersionApi.search
                    .suggestedQueries(query = "a".repeat(101), languageRanges = listOf("en"))
                    .isEmpty()
            }
        }

    @Test
    fun `test suggested queries rejects an empty language range list`() =
        runTest {
            startNoRequestExpected()
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.search.suggestedQueries(query = "love", languageRanges = emptyList())
            }
        }

    @Test
    fun `test trending queries rejects an empty language range list`() =
        runTest {
            startNoRequestExpected()
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.search.trendingQueries(languageRanges = emptyList())
            }
        }

    @Test
    fun `test suggested queries rejects a malformed language range`() =
        runTest {
            startNoRequestExpected()
            listOf("", "en_US", "en-", "-US", "abcdefghi", "en-US!").forEach { languageRange ->
                assertFailsWith<IllegalArgumentException>("expected $languageRange to be rejected") {
                    YouVersionApi.search.suggestedQueries(
                        query = "love",
                        languageRanges = listOf("en", languageRange),
                    )
                }
            }
        }

    @Test
    fun `test trending queries rejects a malformed language range`() =
        runTest {
            startNoRequestExpected()
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.search.trendingQueries(languageRanges = listOf("en_US"))
            }
        }

    private fun startNoRequestExpected() {
        MockEngine { fail("no request should have been issued") }
            .also { engine -> startYouVersionPlatformTest(engine) }
        YouVersionPlatformConfiguration.configure(appKey = "app")
    }
}
