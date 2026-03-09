package com.youversion.platform.core.organizations.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import com.youversion.platform.helpers.testCannotDownload
import com.youversion.platform.helpers.testForbiddenNotPermitted
import com.youversion.platform.helpers.testInvalidResponse
import com.youversion.platform.helpers.testUnauthorizedNotPermitted
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrganizationsApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    // ----- organizations
    @Test
    fun `test organizations success returns data`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/v1/organizations", request.url.encodedPath)
                assertEquals("111", request.url.parameters["bible_id"])
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "abc123",
                                "parent_organization_id": null,
                                "name": "Foo",
                                "description": "Foo Bar",
                                "email": null,
                                "phone": null,
                                "primary_language": "en",
                                "website_url": "https://www.example.com",
                                "address": null
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }
            YouVersionPlatformConfiguration.configure(appKey = "app")

            val orgs = YouVersionApi.organizations.organizations(111)
            val org = orgs[0]
            assertEquals("abc123", org.id)
            assertNull(org.parentOrgId)
            assertEquals("Foo", org.name)
            assertEquals("Foo Bar", org.description)
            assertNull(org.email)
            assertNull(org.phone)
            assertEquals("en", org.primaryLanguage)
            assertEquals("https://www.example.com", org.websiteUrl)
            assertNull(org.address)
        }

    @Test
    fun `test organizations throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.organizations.organizations(111)
        }

    @Test
    fun `test organizations throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.organizations.organizations(111)
        }

    @Test
    fun `test organizations throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.organizations.organizations(111)
        }

    @Test
    fun `test organizations throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.organizations.organizations(111)
        }

    @Test
    fun `test organizations success deserializes non-null optional fields`() =
        runTest {
            MockEngine { request ->
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "xyz789",
                                "parent_organization_id": "parent456",
                                "name": "Bar Org",
                                "description": "A test organization",
                                "email": "test@example.com",
                                "phone": "+1-555-0100",
                                "primary_language": "es",
                                "website_url": "https://www.bar.com",
                                "address": "123 Main St"
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }
            YouVersionPlatformConfiguration.configure(appKey = "app")

            val org = YouVersionApi.organizations.organizations(222)[0]
            assertEquals("xyz789", org.id)
            assertEquals("parent456", org.parentOrgId)
            assertEquals("Bar Org", org.name)
            assertEquals("A test organization", org.description)
            assertEquals("test@example.com", org.email)
            assertEquals("+1-555-0100", org.phone)
            assertEquals("es", org.primaryLanguage)
            assertEquals("https://www.bar.com", org.websiteUrl)
            assertEquals("123 Main St", org.address)
        }

    // ----- organization
    @Test
    fun `test organization success returns object`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/v1/organizations/abc123", request.url.encodedPath)
                respondJson(
                    """
                    {
                        "id": "abc123",
                        "parent_organization_id": null,
                        "name": "Foo",
                        "description": "Foo Bar",
                        "email": null,
                        "phone": null,
                        "primary_language": "en",
                        "website_url": "https://www.example.com",
                        "address": null
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }
            YouVersionPlatformConfiguration.configure(appKey = "app")

            val org = YouVersionApi.organizations.organization("abc123")
            assertEquals("abc123", org.id)
            assertNull(org.parentOrgId)
            assertEquals("Foo", org.name)
            assertEquals("Foo Bar", org.description)
            assertNull(org.email)
            assertNull(org.phone)
            assertEquals("en", org.primaryLanguage)
            assertEquals("https://www.example.com", org.websiteUrl)
            assertNull(org.address)
        }

    @Test
    fun `test organization throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.organizations.organization("abc123")
        }

    @Test
    fun `test organization throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.organizations.organization("abc123")
        }

    @Test
    fun `test organization throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.organizations.organization("abc123")
        }

    @Test
    fun `test organization throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.organizations.organization("abc123")
        }
}
