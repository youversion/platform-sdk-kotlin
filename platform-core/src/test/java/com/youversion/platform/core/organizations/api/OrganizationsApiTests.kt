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
            assertEquals("abc123", orgs[0].id)
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

    // ----- organizations
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
        }

    @Test
    fun `test organization throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted {
            YouVersionApi.organizations.organizations(111)
        }

    @Test
    fun `test organization throws not permitted if forbidden`() =
        testForbiddenNotPermitted {
            YouVersionApi.organizations.organizations(111)
        }

    @Test
    fun `test organization throws cannot download if request failed`() =
        testCannotDownload {
            YouVersionApi.organizations.organizations(111)
        }

    @Test
    fun `test organization throws invalid response if cannot parse`() =
        testInvalidResponse {
            YouVersionApi.organizations.organizations(111)
        }
}
