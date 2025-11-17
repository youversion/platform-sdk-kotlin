package com.youversion.platform.core.languages.api

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguagesEndpointsTests {
    @Test
    fun `test languages url`() {
        with(LanguagesEndpoints.languagesUrl("US")) {
            assertEquals("https://api.youversion.com/v1/languages?country=US", toString())
        }

        with(LanguagesEndpoints.languagesUrl()) {
            assertEquals("https://api.youversion.com/v1/languages", toString())
        }
    }
}
