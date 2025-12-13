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
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UsersEndpointsTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `test authorizeUrl builds correct url`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "test_app_key")
        val permissions = setOf(SignInWithYouVersionPermission.PROFILE, SignInWithYouVersionPermission.EMAIL)
        val pkceParams = SignInWithYouVersionPKCEParameters(
            codeChallenge = "challenge_string",
            codeVerifier = "verifier_string",
            nonce = "nonce_string",
            state = "state_string",
        )

        val url = UsersEndpoints.authorizeUrl(
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
        assertTrue(url.contains("scope=email+openid+profile")) // The implementation sorts and encodes
        assertTrue(url.contains("require_user_interaction=true"))
        assertTrue(url.contains("x-yvp-installation-id=$installId"))
    }

    @Test
    fun `test performRefresh success`() = runTest {
        val mockResponse = """
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
            idToken = "fake.id.token", // This should be preserved
        )

        val result = YouVersionApi.users.performRefresh()

        assertEquals("new_access_token", result.accessToken)
        assertEquals("new_refresh_token", result.refreshToken)
        assertEquals("fake.id.token", result.idToken, "Original idToken should be preserved")
        assertTrue(result.permissions.isEmpty(), "Permissions should be empty for a refresh response")
    }

    @Test
    fun `test performRefresh throws if no refresh token`() = runTest {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "test_app_key", refreshToken = null) // Ensure token is null
        val exception = assertFailsWith<IllegalStateException> {
            YouVersionApi.users.performRefresh()
        }
        assertEquals("Refresh token not available. Cannot perform refresh.", exception.message)
    }
}
