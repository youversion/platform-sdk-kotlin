package com.youversion.platform.core.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PaginatedResponseTests {
    private fun response(
        data: List<String> = listOf("a"),
        nextPageToken: String? = "token",
        totalSize: Int? = 1,
    ) = PaginatedResponse<String>(
        data = data,
        nextPageToken = nextPageToken,
        totalSize = totalSize,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(response(), response())
        assertEquals(response().hashCode(), response().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            response(data = listOf("b")),
            response(nextPageToken = null),
            response(totalSize = 2),
        ).forEach {
            assertNotEquals(response(), it)
            assertNotEquals(response().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("PaginatedResponse(data=[a], nextPageToken=token, totalSize=1)", response().toString())
    }
}
