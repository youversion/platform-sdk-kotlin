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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouVersionApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    private fun configureTestEnvironment(
        mockEngine: MockEngine? = null,
        appKey: String? = "app",
        accessToken: String = "old_token",
        refreshToken: String? = "refresh",
        idToken: String? = null,
        expiryDate: Date? = null,
    ) {
        if (mockEngine != null) startYouVersionPlatformTest(mockEngine) else startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = appKey)
        if (expiryDate != null) {
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = accessToken,
                refreshToken = refreshToken,
                idToken = idToken,
                expiryDate = expiryDate,
                persist = false,
            )
        }
    }

    @Test
    fun `test hasValidToken returns false when expiryDate is null`() =
        runTest {
            configureTestEnvironment()

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken returns true without refresh when expiryDate is far in future`() =
        runTest {
            val engine =
                MockEngine {
                    throw AssertionError("No network request should be made")
                }
            configureTestEnvironment(
                mockEngine = engine,
                accessToken = "token",
                expiryDate = Date(System.currentTimeMillis() + 60_000L),
            )

            assertTrue(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken refreshes and returns true when expiryDate is in the past`() =
        runTest {
            var wasRefreshCalled = false
            val engine =
                MockEngine {
                    wasRefreshCalled = true
                    respondJson(REFRESH_SUCCESS_JSON)
                }
            configureTestEnvironment(
                mockEngine = engine,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
            )

            assertTrue(YouVersionApi.hasValidToken())
            assertEquals("new_access_token", YouVersionPlatformConfiguration.accessToken)
            assertTrue(wasRefreshCalled)
        }

    @Test
    fun `test hasValidToken refreshes and returns true when expiryDate is within 30 seconds`() =
        runTest {
            var wasRefreshCalled = false
            val engine =
                MockEngine {
                    wasRefreshCalled = true
                    respondJson(REFRESH_SUCCESS_JSON)
                }
            configureTestEnvironment(
                mockEngine = engine,
                expiryDate = Date(System.currentTimeMillis() + 15_000L),
            )

            assertTrue(YouVersionApi.hasValidToken())
            assertEquals("new_access_token", YouVersionPlatformConfiguration.accessToken)
            assertTrue(wasRefreshCalled)
        }

    @Test
    fun `test hasValidToken returns false when expiryDate is expired and refresh fails`() =
        runTest {
            val engine =
                MockEngine {
                    respond("", HttpStatusCode.InternalServerError)
                }
            configureTestEnvironment(
                mockEngine = engine,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
            )

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken returns false when refreshToken is null and token is expired`() =
        runTest {
            configureTestEnvironment(
                refreshToken = null,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
            )

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken returns false when appKey is null and token is expired`() =
        runTest {
            configureTestEnvironment(
                appKey = null,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
            )

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken updates configuration after successful refresh`() =
        runTest {
            val engine =
                MockEngine {
                    respondJson(REFRESH_SUCCESS_JSON)
                }
            configureTestEnvironment(
                mockEngine = engine,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
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
            val engine =
                MockEngine {
                    wasRefreshCalled = true
                    respondJson(REFRESH_SUCCESS_JSON)
                }
            configureTestEnvironment(
                mockEngine = engine,
                expiryDate = Date(System.currentTimeMillis() + 30_000L),
            )

            assertTrue(YouVersionApi.hasValidToken())
            assertEquals("new_access_token", YouVersionPlatformConfiguration.accessToken)
            assertTrue(wasRefreshCalled)
        }

    @Test
    fun `test hasValidToken returns false when refresh returns malformed JSON`() =
        runTest {
            val engine =
                MockEngine {
                    respondJson("not valid json")
                }
            configureTestEnvironment(
                mockEngine = engine,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
            )

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken returns false when network error occurs during refresh`() =
        runTest {
            val engine =
                MockEngine {
                    throw IOException("Connection refused")
                }
            configureTestEnvironment(
                mockEngine = engine,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
            )

            assertFalse(YouVersionApi.hasValidToken())
        }

    @Test
    fun `test hasValidToken does not refresh on second call after successful refresh`() =
        runTest {
            val requestCount = AtomicInteger(0)
            val engine =
                MockEngine {
                    requestCount.incrementAndGet()
                    respondJson(REFRESH_SUCCESS_JSON)
                }
            configureTestEnvironment(
                mockEngine = engine,
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
            )

            assertTrue(YouVersionApi.hasValidToken())
            assertTrue(YouVersionApi.hasValidToken())
            assertEquals(1, requestCount.get())
        }

    @Test
    fun `test hasValidToken preserves idToken after successful refresh`() =
        runTest {
            val engine =
                MockEngine {
                    respondJson(REFRESH_SUCCESS_JSON)
                }
            configureTestEnvironment(
                mockEngine = engine,
                idToken = "original_id_token",
                expiryDate = Date(System.currentTimeMillis() - 60_000L),
            )

            assertTrue(YouVersionApi.hasValidToken())
            assertEquals("original_id_token", YouVersionPlatformConfiguration.idToken)
        }

    @Test
    fun `currentSessionId is null when signed out`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertNull(YouVersionApi.currentSessionId)
    }

    @Test
    fun `currentSessionId follows the account when an id token names one`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        signIn(accessToken = "token-a", refreshToken = "refresh-a", idToken = idToken(subject = "user-a"))
        val first = YouVersionApi.currentSessionId
        signIn(accessToken = "token-b", refreshToken = "refresh-b", idToken = idToken(subject = "user-b"))

        assertNotNull(first)
        assertNotEquals(first, YouVersionApi.currentSessionId)
    }

    @Test
    fun `currentSessionId survives a token refresh that keeps the same account`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        signIn(accessToken = "token-a", refreshToken = "refresh-a", idToken = idToken(subject = "user-a"))
        val beforeRefresh = YouVersionApi.currentSessionId
        signIn(accessToken = "token-b", refreshToken = "refresh-b", idToken = idToken(subject = "user-a"))

        assertEquals(beforeRefresh, YouVersionApi.currentSessionId)
    }

    @Test
    fun `currentSessionId distinguishes two signed-in sessions that name no account`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        signIn(accessToken = "token-a", refreshToken = "refresh-a", idToken = null)
        val first = YouVersionApi.currentSessionId
        signIn(accessToken = "token-b", refreshToken = "refresh-b", idToken = null)

        assertNotNull(first)
        assertNull(YouVersionApi.users.currentUserId)
        assertNotEquals(first, YouVersionApi.currentSessionId)
    }

    @Test
    fun `currentSessionId survives an access token refresh for a session that names no account`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        signIn(accessToken = "token-a", refreshToken = "refresh-a", idToken = null)
        val beforeRefresh = YouVersionApi.currentSessionId
        signIn(accessToken = "token-b", refreshToken = "refresh-a", idToken = null)

        assertEquals(beforeRefresh, YouVersionApi.currentSessionId)
    }

    @Test
    fun `currentSessionId does not leak the token it is derived from`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        signIn(accessToken = "token-a", refreshToken = "refresh-a", idToken = null)

        val sessionId = assertNotNull(YouVersionApi.currentSessionId)
        assertFalse(sessionId.contains("refresh-a"))
        assertFalse(sessionId.contains("token-a"))
    }

    private fun signIn(
        accessToken: String,
        refreshToken: String?,
        idToken: String?,
    ) = YouVersionPlatformConfiguration.saveAuthData(
        accessToken = accessToken,
        refreshToken = refreshToken,
        idToken = idToken,
        expiryDate = Date(System.currentTimeMillis() + 3_600_000L),
        persist = false,
    )

    @OptIn(ExperimentalEncodingApi::class)
    private fun idToken(subject: String): String {
        val claims =
            Base64.UrlSafe
                .withPadding(Base64.PaddingOption.ABSENT)
                .encode("""{"sub":"$subject"}""".toByteArray())
        return "header.$claims.signature"
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
