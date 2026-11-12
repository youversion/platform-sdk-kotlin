package com.youversion.platform.core.search.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SearchUserIntentTests {
    private fun intent(rawValue: String = "raw") =
        SearchUserIntent(
            rawValue = rawValue,
        )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(intent(), intent())
        assertEquals(intent().hashCode(), intent().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            intent(rawValue = "other"),
        ).forEach {
            assertNotEquals(intent(), it)
            assertNotEquals(intent().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("SearchUserIntent(rawValue=raw)", intent().toString())
    }
}
