package com.youversion.platform.core.api

import io.ktor.http.HttpStatusCode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.seconds

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
    fun `test the message includes the server message`() {
        assertEquals(
            "server_error (503): Service unavailable",
            YouVersionNetworkException
                .requestFailed(HttpStatusCode.ServiceUnavailable, serverMessage = "Service unavailable")
                .message,
        )
    }

    @Test
    fun `test it survives java serialization`() {
        val issue = YouVersionNetworkException.ValidationIssue(listOf("query", "tag"), "invalid tag")
        val original =
            YouVersionNetworkException.requestFailed(
                HttpStatusCode.ServiceUnavailable,
                serverMessage = "Service unavailable",
                retryAfter = 30.seconds,
                validationIssues = listOf(issue),
            )
        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(original) } }
        val copy =
            assertIs<YouVersionNetworkException>(
                ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() },
            )

        assertSame(YouVersionNetworkException.Reason.SERVER_ERROR, copy.reason)
        assertEquals(original.message, copy.message)
        assertEquals(503, copy.statusCode)
        assertEquals(30.seconds, copy.retryAfter)
        assertEquals(listOf(issue), copy.validationIssues)
    }

    @Test
    fun `test validation issues compare by value`() {
        assertEquals(
            YouVersionNetworkException.ValidationIssue(listOf("query"), "invalid"),
            YouVersionNetworkException.ValidationIssue(listOf("query"), "invalid"),
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
