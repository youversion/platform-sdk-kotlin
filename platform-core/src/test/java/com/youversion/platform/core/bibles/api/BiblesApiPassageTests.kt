package com.youversion.platform.core.bibles.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleReference
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

class BiblesApiPassageTests : YouVersionPlatformTest {
    val reference = BibleReference(versionId = 206, bookUSFM = "JHN", chapter = 3, verse = 1)

    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    // ----- Passage
    @Test
    fun `test passage success returns decoded passage`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(request.url.encodedPath, "/v1/bibles/206/passages/JHN.3.1")
                respondJson(
                    """
                    {
                        "id": "JHN.3.1",
                        "content": "content",
                        "reference": "John 3:1"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val passage = YouVersionApi.bible.passage(reference = reference)

            assertEquals("JHN.3.1", passage.id)
        }

    @Test
    fun `test passage throws not permitted if unauthorized`() =
        testUnauthorizedNotPermitted { YouVersionApi.bible.passage(reference) }

    @Test
    fun `test passage throws not permitted if forbidden`() =
        testForbiddenNotPermitted { YouVersionApi.bible.passage(reference) }

    @Test
    fun `test passage throws cannot download if request failed`() =
        testCannotDownload { YouVersionApi.bible.passage(reference) }

    @Test
    fun `test passage throws invalid response if cannot parse`() =
        testInvalidResponse { YouVersionApi.bible.passage(reference) }
}
