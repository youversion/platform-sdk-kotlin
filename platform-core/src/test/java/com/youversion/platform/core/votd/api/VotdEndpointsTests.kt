package com.youversion.platform.core.votd.api

import kotlin.test.Test
import kotlin.test.assertEquals

class VotdEndpointsTests {
    @Test
    fun `test votd url`() {
        assertEquals(
            "https://api.youversion.com/v1/verse-of-the-days/1",
            VotdEndpoints.votdUrl(1),
        )

        assertEquals(
            "https://api.youversion.com/v1/verse-of-the-days",
            VotdEndpoints.votdUrl(),
        )
    }

    @Test
    fun `test votd url repeats language ranges in order`() {
        assertEquals(
            "https://api.youversion.com/v1/verse-of-the-days/1" +
                "?language_ranges%5B%5D=es-MX&language_ranges%5B%5D=en",
            VotdEndpoints.votdUrl(1, listOf("es-MX", "en")),
        )

        assertEquals(
            "https://api.youversion.com/v1/verse-of-the-days?language_ranges%5B%5D=en",
            VotdEndpoints.votdUrl(languageRanges = listOf("en")),
        )
    }
}
