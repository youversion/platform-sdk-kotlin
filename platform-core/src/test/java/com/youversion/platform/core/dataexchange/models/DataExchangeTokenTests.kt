package com.youversion.platform.core.dataexchange.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DataExchangeTokenTests {
    private fun token(token: String = "token") =
        DataExchangeToken(
            token = token,
        )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(token(), token())
        assertEquals(token().hashCode(), token().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            token(token = "other"),
        ).forEach {
            assertNotEquals(token(), it)
            assertNotEquals(token().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString masks the token`() {
        assertEquals("DataExchangeToken(token=<redacted>)", token().toString())
    }
}
