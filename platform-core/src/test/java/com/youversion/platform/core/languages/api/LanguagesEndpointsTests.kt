package com.youversion.platform.core.languages.api

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguagesEndpointsTests {
    @Test
    fun `test languages url`() {
        assertEquals(
            "https://api.youversion.com/v1/languages?country=US&page_size=25&page_token=token",
            LanguagesEndpoints.languagesUrl("US", 25, "token"),
        )
        assertEquals(
            "https://api.youversion.com/v1/languages",
            LanguagesEndpoints.languagesUrl(),
        )
    }
}
