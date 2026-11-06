package com.youversion.platform.core.bibles.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CachedBibleContentTests {
    private fun content(
        value: String = "value",
        expiresAt: Long? = 1L,
    ) = CachedBibleContent<String>(
        value = value,
        expiresAt = expiresAt,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(content(), content())
        assertEquals(content().hashCode(), content().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            content(value = "other"),
            content(expiresAt = null),
        ).forEach {
            assertNotEquals(content(), it)
            assertNotEquals(content().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("CachedBibleContent(value=value, expiresAt=1)", content().toString())
    }
}
