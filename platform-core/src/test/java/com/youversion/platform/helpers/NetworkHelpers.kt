package com.youversion.platform.helpers

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionNetworkException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

fun MockRequestHandleScope.respondJson(
    content: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData =
    respond(
        content = content,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

fun testUnauthorizedNotPermitted(block: suspend () -> Unit) =
    runTest {
        MockEngine { request ->
            respondJson("""{"message":"Invalid app key"}""", HttpStatusCode.Unauthorized)
        }.also { engine -> startYouVersionPlatformTest(engine) }

        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertFailsWith<YouVersionNetworkException> { block() }
            .apply {
                assertEquals(YouVersionNetworkException.Reason.NOT_PERMITTED, reason)
                assertEquals(401, statusCode)
                assertEquals("Invalid app key", serverMessage)
            }
    }

fun testForbiddenNotPermitted(block: suspend () -> Unit) =
    runTest {
        MockEngine { request ->
            respondJson("""{"message":"App key lacks access"}""", HttpStatusCode.Forbidden)
        }.also { engine -> startYouVersionPlatformTest(engine) }

        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertFailsWith<YouVersionNetworkException> { block() }
            .apply {
                assertEquals(YouVersionNetworkException.Reason.NOT_PERMITTED, reason)
                assertEquals(403, statusCode)
                assertEquals("App key lacks access", serverMessage)
            }
    }

fun testServerError(block: suspend () -> Unit) =
    runTest {
        MockEngine { request ->
            respond(
                content = """{"message":"Service unavailable"}""",
                status = HttpStatusCode.ServiceUnavailable,
                headers =
                    headersOf(
                        HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                        HttpHeaders.RetryAfter to listOf("30"),
                    ),
            )
        }.also { engine -> startYouVersionPlatformTest(engine) }

        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertFailsWith<YouVersionNetworkException> { block() }
            .apply {
                assertEquals(YouVersionNetworkException.Reason.SERVER_ERROR, reason)
                assertEquals(503, statusCode)
                assertEquals("Service unavailable", serverMessage)
                assertEquals(30.seconds, retryAfter)
                assertEquals("server_error (503): Service unavailable", message)
            }
    }

fun testInvalidResponse(block: suspend () -> Unit) =
    runTest {
        MockEngine { request ->
            respond("invalid json", HttpStatusCode.OK)
        }.also { engine -> startYouVersionPlatformTest(engine) }

        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertFailsWith<YouVersionNetworkException> { block() }
            .apply { assertEquals(YouVersionNetworkException.Reason.INVALID_RESPONSE, reason) }
    }
