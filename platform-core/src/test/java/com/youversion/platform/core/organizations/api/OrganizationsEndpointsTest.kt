package com.youversion.platform.core.organizations.api

import kotlin.test.Test
import kotlin.test.assertEquals

class OrganizationsEndpointsTest {
    @Test
    fun `test organizationsUrl`() {
        assertEquals(
            "https://api.youversion.com/v1/organizations?bible_id=111",
            OrganizationsEndpoints.organizationsUrl(111),
        )
    }

    @Test
    fun `test organizationUrl`() {
        assertEquals(
            "https://api.youversion.com/v1/organizations/abc-123",
            OrganizationsEndpoints.organizationUrl("abc-123"),
        )
    }
}
