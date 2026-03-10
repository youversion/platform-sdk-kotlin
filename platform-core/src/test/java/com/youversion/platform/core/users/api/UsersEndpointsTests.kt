package com.youversion.platform.core.users.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.users.model.SignInWithYouVersionPKCEParameters
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class UsersEndpointsTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    // ----- URL Construction

    @Test
    fun `test callbackUrl builds correct url`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertEquals(
            "https://api.youversion.com/auth/callback",
            UsersEndpoints.callbackUrl(),
        )
    }

    @Test
    fun `test userUrl builds correct url`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertEquals(
            "https://api.youversion.com/auth/me?lat=my_token",
            UsersEndpoints.userUrl("my_token"),
        )
    }

    @Test
    fun `test authorizeUrl builds correct url`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "test_app_key")
        val permissions = setOf(SignInWithYouVersionPermission.PROFILE, SignInWithYouVersionPermission.EMAIL)
        val pkceParams =
            SignInWithYouVersionPKCEParameters(
                codeChallenge = "challenge_string",
                codeVerifier = "verifier_string",
                nonce = "nonce_string",
                state = "state_string",
            )

        val url =
            UsersEndpoints.authorizeUrl(
                appKey = YouVersionPlatformConfiguration.appKey!!,
                permissions = permissions,
                redirectUri = "app://callback",
                parameters = pkceParams,
            )

        val installId = YouVersionPlatformConfiguration.installId

        assertTrue(url.startsWith("https://api.youversion.com/auth/authorize?"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("client_id=test_app_key"))
        assertTrue(url.contains("redirect_uri=app%3A%2F%2Fcallback"))
        assertTrue(url.contains("nonce=nonce_string"))
        assertTrue(url.contains("state=state_string"))
        assertTrue(url.contains("code_challenge=challenge_string"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("scope=email+openid+profile"))
        assertTrue(url.contains("require_user_interaction=true"))
        assertTrue(url.contains("x-yvp-installation-id=$installId"))
    }

    @Test
    fun `test authorizeUrl scope always includes openid`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")
        val pkceParams =
            SignInWithYouVersionPKCEParameters(
                codeChallenge = "c",
                codeVerifier = "v",
                nonce = "n",
                state = "s",
            )

        val url =
            UsersEndpoints.authorizeUrl(
                appKey = "app",
                permissions = setOf(SignInWithYouVersionPermission.PROFILE),
                redirectUri = "app://callback",
                parameters = pkceParams,
            )

        assertTrue(url.contains("scope=openid+profile"))
    }

    // ----- signOut

    @Test
    fun `test signOut clears authentication data`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(
            appKey = "app",
            accessToken = "access",
            refreshToken = "refresh",
            idToken = "id",
        )

        assertEquals("access", YouVersionPlatformConfiguration.accessToken)

        UsersEndpoints.signOut()

        assertNull(YouVersionPlatformConfiguration.accessToken)
        assertNull(YouVersionPlatformConfiguration.refreshToken)
        assertNull(YouVersionPlatformConfiguration.idToken)
    }

    // ----- decodeJWT

    @Test
    fun `test decodeJWT valid token returns correct claims`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        val token =
            buildTestJwt(
                buildJsonObject {
                    put("sub", "user123")
                    put("name", "Test User")
                    put("nonce", "my_nonce")
                },
            )

        val claims = YouVersionApi.users.decodeJWT(token)

        assertEquals("user123", claims["sub"])
        assertEquals("Test User", claims["name"])
        assertEquals("my_nonce", claims["nonce"])
    }

    @Test
    fun `test decodeJWT invalid segment count returns empty map`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertEquals(emptyMap(), YouVersionApi.users.decodeJWT("only.two"))
        assertEquals(emptyMap(), YouVersionApi.users.decodeJWT("nosegments"))
    }

    @Test
    fun `test decodeJWT malformed base64 returns empty map`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertEquals(emptyMap(), YouVersionApi.users.decodeJWT("a.!!!notbase64!!!.c"))
    }

    @Test
    fun `test decodeJWT valid base64 invalid json returns empty map`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        val notJson =
            Base64.UrlSafe
                .withPadding(Base64.PaddingOption.ABSENT)
                .encode("not json".toByteArray())
        assertEquals(emptyMap(), YouVersionApi.users.decodeJWT("a.$notJson.c"))
    }

    @Test
    fun `test decodeJWT empty string returns empty map`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        assertEquals(emptyMap(), YouVersionApi.users.decodeJWT(""))
    }

    // ----- getSignInResult

    @Test
    fun `test getSignInResult success flow`() =
        runTest {
            val jwtClaims =
                buildJsonObject {
                    put("nonce", TEST_NONCE)
                    put("sub", "user_42")
                    put("name", "Jane Doe")
                    put("picture", "https://example.com/pic.jpg")
                    put("email", "jane@example.com")
                }
            val idToken = buildTestJwt(jwtClaims)
            val tokenResponseJson = buildTokenResponseJson(idToken = idToken)

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.contains("/auth/callback") ->
                            respond(
                                "",
                                HttpStatusCode.Found,
                                headersOf("Location", "https://redirect.example.com?code=$TEST_AUTH_CODE"),
                            )
                        request.url.encodedPath.contains("/auth/token") ->
                            respondJson(tokenResponseJson)
                        else -> respond("", HttpStatusCode.OK)
                    }
                }
            startYouVersionPlatformTest(engine = mockEngine)
            YouVersionPlatformConfiguration.configure(appKey = TEST_APP_KEY)

            val callbackUri = "${UsersEndpoints.callbackUrl()}?state=$TEST_STATE&session_state=abc"

            val result =
                YouVersionApi.users.getSignInResult(
                    callbackUri = callbackUri,
                    state = TEST_STATE,
                    codeVerifier = TEST_CODE_VERIFIER,
                    redirectUri = TEST_REDIRECT_URI,
                    nonce = TEST_NONCE,
                )

            assertEquals("test_access_token", result.accessToken)
            assertEquals("test_refresh_token", result.refreshToken)
            assertEquals(idToken, result.idToken)
            assertEquals("user_42", result.yvpUserId)
            assertEquals("Jane Doe", result.name)
            assertEquals("https://example.com/pic.jpg", result.profilePicture)
            assertEquals("jane@example.com", result.email)
        }

    @Test
    fun `test getSignInResult throws on state mismatch`() =
        runTest {
            startYouVersionPlatformTest()
            YouVersionPlatformConfiguration.configure(appKey = TEST_APP_KEY)

            val callbackUri = "${UsersEndpoints.callbackUrl()}?state=wrong_state"

            val exception =
                assertFailsWith<IllegalStateException> {
                    YouVersionApi.users.getSignInResult(
                        callbackUri = callbackUri,
                        state = "correct_state",
                        codeVerifier = TEST_CODE_VERIFIER,
                        redirectUri = TEST_REDIRECT_URI,
                        nonce = TEST_NONCE,
                    )
                }
            assertEquals("State mismatch", exception.message)
        }

    @Test
    fun `test getSignInResult throws when callback does not return 302`() =
        runTest {
            val mockEngine = MockEngine { respond("", HttpStatusCode.OK) }
            startYouVersionPlatformTest(engine = mockEngine)
            YouVersionPlatformConfiguration.configure(appKey = TEST_APP_KEY)

            val callbackUri = "${UsersEndpoints.callbackUrl()}?state=$TEST_STATE"

            val exception =
                assertFailsWith<Exception> {
                    YouVersionApi.users.getSignInResult(
                        callbackUri = callbackUri,
                        state = TEST_STATE,
                        codeVerifier = TEST_CODE_VERIFIER,
                        redirectUri = TEST_REDIRECT_URI,
                        nonce = TEST_NONCE,
                    )
                }
            assertTrue(exception.message!!.contains("Expected status 302"))
        }

    @Test
    fun `test getSignInResult throws when no Location header in 302 response`() =
        runTest {
            val mockEngine = MockEngine { respond("", HttpStatusCode.Found) }
            startYouVersionPlatformTest(engine = mockEngine)
            YouVersionPlatformConfiguration.configure(appKey = TEST_APP_KEY)

            val callbackUri = "${UsersEndpoints.callbackUrl()}?state=$TEST_STATE"

            val exception =
                assertFailsWith<Exception> {
                    YouVersionApi.users.getSignInResult(
                        callbackUri = callbackUri,
                        state = TEST_STATE,
                        codeVerifier = TEST_CODE_VERIFIER,
                        redirectUri = TEST_REDIRECT_URI,
                        nonce = TEST_NONCE,
                    )
                }
            assertTrue(exception.message!!.contains("Location header not found"))
        }

    @Test
    fun `test getSignInResult throws when code param absent from location`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.contains("/auth/callback") ->
                            respond(
                                "",
                                HttpStatusCode.Found,
                                headersOf("Location", "https://redirect.example.com?nocode=here"),
                            )
                        else -> respond("", HttpStatusCode.OK)
                    }
                }
            startYouVersionPlatformTest(engine = mockEngine)
            YouVersionPlatformConfiguration.configure(appKey = TEST_APP_KEY)

            val callbackUri = "${UsersEndpoints.callbackUrl()}?state=$TEST_STATE"

            val exception =
                assertFailsWith<Exception> {
                    YouVersionApi.users.getSignInResult(
                        callbackUri = callbackUri,
                        state = TEST_STATE,
                        codeVerifier = TEST_CODE_VERIFIER,
                        redirectUri = TEST_REDIRECT_URI,
                        nonce = TEST_NONCE,
                    )
                }
            assertTrue(exception.message!!.contains("Code not found"))
        }

    @Test
    fun `test getSignInResult throws on nonce mismatch`() =
        runTest {
            val jwtClaims = buildJsonObject { put("nonce", "wrong_nonce") }
            val idToken = buildTestJwt(jwtClaims)
            val tokenResponseJson = buildTokenResponseJson(idToken = idToken)

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.contains("/auth/callback") ->
                            respond(
                                "",
                                HttpStatusCode.Found,
                                headersOf("Location", "https://redirect.example.com?code=$TEST_AUTH_CODE"),
                            )
                        request.url.encodedPath.contains("/auth/token") ->
                            respondJson(tokenResponseJson)
                        else -> respond("", HttpStatusCode.OK)
                    }
                }
            startYouVersionPlatformTest(engine = mockEngine)
            YouVersionPlatformConfiguration.configure(appKey = TEST_APP_KEY)

            val callbackUri = "${UsersEndpoints.callbackUrl()}?state=$TEST_STATE"

            val exception =
                assertFailsWith<IllegalStateException> {
                    YouVersionApi.users.getSignInResult(
                        callbackUri = callbackUri,
                        state = TEST_STATE,
                        codeVerifier = TEST_CODE_VERIFIER,
                        redirectUri = TEST_REDIRECT_URI,
                        nonce = "expected_nonce",
                    )
                }
            assertTrue(exception.message!!.contains("Nonce mismatch"))
        }

    @Test
    fun `test getSignInResult throws when JWT has no nonce claim`() =
        runTest {
            val jwtClaims = buildJsonObject { put("sub", "user123") }
            val idToken = buildTestJwt(jwtClaims)
            val tokenResponseJson = buildTokenResponseJson(idToken = idToken)

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.contains("/auth/callback") ->
                            respond(
                                "",
                                HttpStatusCode.Found,
                                headersOf("Location", "https://redirect.example.com?code=$TEST_AUTH_CODE"),
                            )
                        request.url.encodedPath.contains("/auth/token") ->
                            respondJson(tokenResponseJson)
                        else -> respond("", HttpStatusCode.OK)
                    }
                }
            startYouVersionPlatformTest(engine = mockEngine)
            YouVersionPlatformConfiguration.configure(appKey = TEST_APP_KEY)

            val callbackUri = "${UsersEndpoints.callbackUrl()}?state=$TEST_STATE"

            val exception =
                assertFailsWith<IllegalStateException> {
                    YouVersionApi.users.getSignInResult(
                        callbackUri = callbackUri,
                        state = TEST_STATE,
                        codeVerifier = TEST_CODE_VERIFIER,
                        redirectUri = TEST_REDIRECT_URI,
                        nonce = TEST_NONCE,
                    )
                }
            assertTrue(exception.message!!.contains("Nonce mismatch"))
        }

    // ----- performRefresh

    @Test
    fun `test performRefresh success`() =
        runTest {
            val mockResponse =
                """
                {
                  "access_token": "new_access_token",
                  "expires_in": 3600,
                  "refresh_token": "new_refresh_token",
                  "scope": "scope"
                }
                """.trimIndent()
            val mockEngine = MockEngine { respondJson(mockResponse) }
            startYouVersionPlatformTest(engine = mockEngine)
            YouVersionPlatformConfiguration.configure(
                appKey = "test_app_key",
                refreshToken = "old_refresh_token",
                idToken = "fake.id.token",
            )

            val result = YouVersionApi.users.performRefresh()

            assertEquals("new_access_token", result.accessToken)
            assertEquals("new_refresh_token", result.refreshToken)
            assertEquals(
                "fake.id.token",
                result.idToken,
                "Original idToken should be preserved",
            )
            assertTrue(
                result.permissions.isEmpty(),
                "Permissions should be empty for a refresh response",
            )
        }

    @Test
    fun `test performRefresh throws if no refresh token`() =
        runTest {
            startYouVersionPlatformTest()
            YouVersionPlatformConfiguration.configure(appKey = "test_app_key", refreshToken = null)
            val exception =
                assertFailsWith<IllegalStateException> {
                    YouVersionApi.users.performRefresh()
                }
            assertEquals("Refresh token not available. Cannot perform refresh.", exception.message)
        }

    @Test
    fun `test performRefresh throws if no app key`() =
        runTest {
            startYouVersionPlatformTest()
            YouVersionPlatformConfiguration.configure(appKey = null, refreshToken = "some_token")
            val exception =
                assertFailsWith<IllegalStateException> {
                    YouVersionApi.users.performRefresh()
                }
            assertEquals("App key not configured. Cannot perform refresh.", exception.message)
        }
}

private const val TEST_STATE = "test_state"
private const val TEST_NONCE = "test_nonce"
private const val TEST_CODE_VERIFIER = "test_verifier"
private const val TEST_REDIRECT_URI = "app://callback"
private const val TEST_AUTH_CODE = "auth_code_123"
private const val TEST_APP_KEY = "test_app_key"

@OptIn(ExperimentalEncodingApi::class)
private fun buildTestJwt(claims: JsonObject): String {
    val payload = claims.toString().toByteArray()
    val encodedPayload = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(payload)
    return "eyJhbGciOiJSUzI1NiJ9.$encodedPayload.fake_signature"
}

private fun buildTokenResponseJson(
    idToken: String,
    accessToken: String = "test_access_token",
    refreshToken: String = "test_refresh_token",
    expiresIn: Long = 3600,
    scope: String = "openid,profile,email",
): String =
    """
    {
      "access_token": "$accessToken",
      "expires_in": $expiresIn,
      "id_token": "$idToken",
      "refresh_token": "$refreshToken",
      "scope": "$scope",
      "token_type": "Bearer"
    }
    """.trimIndent()
