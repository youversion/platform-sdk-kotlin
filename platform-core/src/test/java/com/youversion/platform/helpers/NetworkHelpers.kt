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
            respond("", HttpStatusCode.Unauthorized)
        }.also { engine -> startYouVersionPlatformTest(engine) }

        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertFailsWith<YouVersionNetworkException> { block() }
            .apply { assertEquals(YouVersionNetworkException.Reason.NOT_PERMITTED, reason) }
    }

fun testForbiddenNotPermitted(block: suspend () -> Unit) =
    runTest {
        MockEngine { request ->
            respond("", HttpStatusCode.Forbidden)
        }.also { engine -> startYouVersionPlatformTest(engine) }

        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertFailsWith<YouVersionNetworkException> { block() }
            .apply { assertEquals(YouVersionNetworkException.Reason.NOT_PERMITTED, reason) }
    }

fun testCannotDownload(block: suspend () -> Unit) =
    runTest {
        MockEngine { request ->
            respond("", HttpStatusCode.InternalServerError)
        }.also { engine -> startYouVersionPlatformTest(engine) }

        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertFailsWith<YouVersionNetworkException> { block() }
            .apply { assertEquals(YouVersionNetworkException.Reason.CANNOT_DOWNLOAD, reason) }
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
