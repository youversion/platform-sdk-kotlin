package com.youversion.platform.core.highlights.api

import kotlin.test.Test
import kotlin.test.assertEquals

class HighlightsEndpointsTests {
    @Test
    fun `test highlightsUrl`() {
        with(HighlightsEndpoints.highlightsUrl()) {
            assertEquals("https://api-staging.youversion.com/v1/highlights", toString())
        }

        with(HighlightsEndpoints.highlightsUrl(versionId = 1, passageId = "GEN.1")) {
            assertEquals("https://api-staging.youversion.com/v1/highlights?version_id=1&passage_id=GEN.1", toString())
        }
    }

    @Test
    fun `test highlightsDeleteUrl`() {
        with(HighlightsEndpoints.highlightsDeleteUrl(versionId = 1, passageId = "GEN.1")) {
            assertEquals("https://api-staging.youversion.com/v1/highlights/GEN.1?version_id=1", toString())
        }
    }
}
