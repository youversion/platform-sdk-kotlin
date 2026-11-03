package com.youversion.platform.core.api

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YouVersionNetworkExceptionTests {
    @Test
    fun `test cannot download is always the reason`() {
        assertEquals(YouVersionNetworkException.Reason.CANNOT_DOWNLOAD, cannotDownload(HttpStatusCode.NotFound).reason)
    }

    @Test
    fun `test 429 is rate limited`() {
        assertEquals(
            YouVersionNetworkException.Failure.RATE_LIMITED,
            cannotDownload(HttpStatusCode.TooManyRequests).failure,
        )
    }

    @Test
    fun `test 5xx is a server error`() {
        listOf(500, 503, 599).forEach {
            assertEquals(
                YouVersionNetworkException.Failure.SERVER_ERROR,
                cannotDownload(HttpStatusCode.fromValue(it)).failure,
            )
        }
    }

    @Test
    fun `test other 4xx is request rejected`() {
        listOf(400, 404, 422, 499).forEach {
            assertEquals(
                YouVersionNetworkException.Failure.REQUEST_REJECTED,
                cannotDownload(HttpStatusCode.fromValue(it)).failure,
            )
        }
    }

    @Test
    fun `test an unclassified status has no failure`() {
        listOf(200, 304, 600).forEach { assertNull(cannotDownload(HttpStatusCode.fromValue(it)).failure) }
    }

    @Test
    fun `test the public constructor has no failure`() {
        assertNull(YouVersionNetworkException(YouVersionNetworkException.Reason.NOT_PERMITTED).failure)
    }
}
