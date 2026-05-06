package com.youversion.platform.core.utilities.koin

import com.youversion.platform.core.BuildConfig
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultRequestHeadersTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `test x-yvp-sdk header is sent on every request`() =
        runTest {
            var capturedHeader: String? = null
            MockEngine { request ->
                capturedHeader = request.headers["x-yvp-sdk"]
                respondJson("""{"day":1,"passage_id":"ISA.43.19"}""")
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionApi.votd.verseOfTheDay(1)

            assertEquals("KotlinSDK=${BuildConfig.SDK_VERSION}", capturedHeader)
        }
}
