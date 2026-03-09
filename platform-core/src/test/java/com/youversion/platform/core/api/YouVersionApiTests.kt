package com.youversion.platform.core.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YouVersionApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `test hasValidToken returns false when expiryDate is null`() =
        runTest {
            startYouVersionPlatformTest()

            YouVersionPlatformConfiguration.configure(appKey = "app")

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken returns true without refresh when expiryDate is far in future`() =
        runTest {
            MockEngine {
                throw AssertionError("No network request should be made")
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "token",
                refreshToken = "refresh",
                idToken = null,
                expiryDate = Date(System.currentTimeMillis() + 60_000L),
                persist = false,
            )

            assertTrue(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken refreshes and returns true when expiryDate is in the past`() =
        runTest {
            MockEngine {
                respondJson(REFRESH_SUCCESS_JSON)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "old_token",
                refreshToken = "refresh",
                idToken = null,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
                persist = false,
            )

            assertTrue(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken refreshes and returns true when expiryDate is within 30 seconds`() =
        runTest {
            MockEngine {
                respondJson(REFRESH_SUCCESS_JSON)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "old_token",
                refreshToken = "refresh",
                idToken = null,
                expiryDate = Date(System.currentTimeMillis() + 15_000L),
                persist = false,
            )

            assertTrue(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken returns false when expiryDate is expired and refresh fails`() =
        runTest {
            MockEngine {
                respond("", HttpStatusCode.InternalServerError)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "old_token",
                refreshToken = "refresh",
                idToken = null,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
                persist = false,
            )

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken returns false when refreshToken is null and token is expired`() =
        runTest {
            startYouVersionPlatformTest()

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "old_token",
                refreshToken = null,
                idToken = null,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
                persist = false,
            )

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken returns false when appKey is null and token is expired`() =
        runTest {
            startYouVersionPlatformTest()

            YouVersionPlatformConfiguration.configure(appKey = null)
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "old_token",
                refreshToken = "refresh",
                idToken = null,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
                persist = false,
            )

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken updates configuration after successful refresh`() =
        runTest {
            MockEngine {
                respondJson(REFRESH_SUCCESS_JSON)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "old_token",
                refreshToken = "refresh",
                idToken = null,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
                persist = false,
            )

            assertTrue(YouVersionApi.hasValidToken())

            assertEquals("new_access_token", YouVersionPlatformConfiguration.accessToken)
            assertEquals("new_refresh_token", YouVersionPlatformConfiguration.refreshToken)
            assertNotNull(YouVersionPlatformConfiguration.expiryDate)
        }

    @Test
    fun `test hasValidToken refreshes when expiryDate is exactly 30 seconds from now`() =
        runTest {
            var wasRefreshCalled = false
            MockEngine {
                wasRefreshCalled = true
                respondJson(REFRESH_SUCCESS_JSON)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "old_token",
                refreshToken = "refresh",
                idToken = null,
                expiryDate = Date(System.currentTimeMillis() + 30_000L),
                persist = false,
            )

            assertTrue(YouVersionApi.hasValidToken())
            assertTrue(wasRefreshCalled)
        }

    @Test
    fun `test hasValidToken returns false when refresh returns malformed JSON`() =
        runTest {
            MockEngine {
                respondJson("not valid json")
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "old_token",
                refreshToken = "refresh",
                idToken = null,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
                persist = false,
            )

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken returns false when network error occurs during refresh`() =
        runTest {
            MockEngine {
                throw IOException("Connection refused")
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "old_token",
                refreshToken = "refresh",
                idToken = null,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
                persist = false,
            )

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken does not refresh on second call after successful refresh`() =
        runTest {
            val requestCount = AtomicInteger(0)
            MockEngine {
                requestCount.incrementAndGet()
                respondJson(REFRESH_SUCCESS_JSON)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "old_token",
                refreshToken = "refresh",
                idToken = null,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
                persist = false,
            )

            assertTrue(YouVersionApi.hasValidToken())
            assertTrue(YouVersionApi.hasValidToken())
            assertEquals(1, requestCount.get())
        }

    @Test
    fun `test hasValidToken preserves idToken after successful refresh`() =
        runTest {
            MockEngine {
                respondJson(REFRESH_SUCCESS_JSON)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "old_token",
                refreshToken = "refresh",
                idToken = "original_id_token",
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
                persist = false,
            )

            assertTrue(YouVersionApi.hasValidToken())
            assertEquals("original_id_token", YouVersionPlatformConfiguration.idToken)
        }
}

private const val REFRESH_SUCCESS_JSON = """
{
    "access_token": "new_access_token",
    "expires_in": 3600,
    "refresh_token": "new_refresh_token",
    "scope": "openid"
}
"""
