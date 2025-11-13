package com.youversion.platform.core.votd.api

import kotlin.test.Test
import kotlin.test.assertEquals

class VotdEndpointsTests {
    @Test
    fun `test votd url`() {
        assertEquals(
            "https://api-staging.youversion.com/v1/verse_of_the_days/1",
            VotdEndpoints.votdUrl(1).toString(),
        )

        assertEquals(
            "https://api-staging.youversion.com/v1/verse_of_the_days",
            VotdEndpoints.votdUrl().toString(),
        )
    }
}
