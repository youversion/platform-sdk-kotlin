package com.youversion.platform.core.bibles.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleContentResponseTests {
    private val now = 1_000L

    private suspend fun responseWithHeaders(headers: Headers = Headers.Empty): HttpResponse {
        val engine = MockEngine { respond(content = "", headers = headers) }
        return HttpClient(engine).get("https://test")
    }

    @Test
    fun `test expiresAt defaults to seven days without a Cache-Control header`() =
        runTest {
            val response = responseWithHeaders()

            assertEquals(now + DEFAULT_CACHE_DURATION_MILLIS, response.cacheExpiresAt(now))
        }

    @Test
    fun `test expiresAt honors max-age`() =
        runTest {
            val response = responseWithHeaders(headersOf("Cache-Control", "public, max-age=3600"))

            assertEquals(now + 3_600_000, response.cacheExpiresAt(now))
        }

    @Test
    fun `test expiresAt subtracts the Age header from max-age`() =
        runTest {
            val response =
                responseWithHeaders(
                    headersOf(
                        "Cache-Control" to listOf("max-age=3600"),
                        "Age" to listOf("600"),
                    ),
                )

            assertEquals(now + 3_000_000, response.cacheExpiresAt(now))
        }

    @Test
    fun `test expiresAt is now when Age exceeds max-age`() =
        runTest {
            val response =
                responseWithHeaders(
                    headersOf(
                        "Cache-Control" to listOf("max-age=60"),
                        "Age" to listOf("600"),
                    ),
                )

            assertEquals(now, response.cacheExpiresAt(now))
        }

    @Test
    fun `test allowsCaching is true for a cacheable response`() =
        runTest {
            assertTrue(responseWithHeaders().allowsCaching)
            assertTrue(responseWithHeaders(headersOf("Cache-Control", "public, max-age=3600")).allowsCaching)
        }

    @Test
    fun `test no-store disallows caching`() =
        runTest {
            assertFalse(responseWithHeaders(headersOf("Cache-Control", "no-store")).allowsCaching)
        }

    @Test
    fun `test no-cache disallows caching`() =
        runTest {
            assertFalse(responseWithHeaders(headersOf("Cache-Control", "No-Cache, max-age=3600")).allowsCaching)
        }
}
