package com.youversion.platform.core.api

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class YouVersionNetworkExceptionTests {
    @Test
    fun `test 429 is rate limited`() {
        assertEquals(
            YouVersionNetworkException.Reason.RATE_LIMITED,
            YouVersionNetworkException.requestFailed(HttpStatusCode.TooManyRequests).reason,
        )
    }

    @Test
    fun `test 5xx is a server error`() {
        listOf(500, 503, 599).forEach {
            assertEquals(
                YouVersionNetworkException.Reason.SERVER_ERROR,
                YouVersionNetworkException.requestFailed(HttpStatusCode.fromValue(it)).reason,
            )
        }
    }

    @Test
    fun `test other 4xx is request rejected`() {
        listOf(400, 404, 422, 499).forEach {
            assertEquals(
                YouVersionNetworkException.Reason.REQUEST_REJECTED,
                YouVersionNetworkException.requestFailed(HttpStatusCode.fromValue(it)).reason,
            )
        }
    }

    @Test
    fun `test an unclassified status is request failed`() {
        listOf(200, 304, 600).forEach {
            assertEquals(
                YouVersionNetworkException.Reason.REQUEST_FAILED,
                YouVersionNetworkException.requestFailed(HttpStatusCode.fromValue(it)).reason,
            )
        }
    }

    @Test
    fun `test the message names the reason and status`() {
        assertEquals(
            "server_error (503)",
            YouVersionNetworkException.requestFailed(HttpStatusCode.ServiceUnavailable).message,
        )
    }

    @Test
    fun `test the public constructor uses the reason as the message`() {
        assertEquals(
            "not_permitted",
            YouVersionNetworkException(YouVersionNetworkException.Reason.NOT_PERMITTED).message,
        )
    }
}
