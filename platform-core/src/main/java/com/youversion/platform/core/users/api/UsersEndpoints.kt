package com.youversion.platform.core.users.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.users.model.RefreshTokenResponse
import com.youversion.platform.core.users.model.SignInWithYouVersionPKCEParameters
import com.youversion.platform.core.users.model.SignInWithYouVersionResult
import com.youversion.platform.core.users.model.TokenResponse
import com.youversion.platform.core.users.model.YouVersionPermissions
import com.youversion.platform.core.users.model.YouVersionUserInfo
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.parameters
import io.ktor.http.path
import io.ktor.http.takeFrom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal object UsersEndpoints : UsersApi {
    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

    /**
     * Permissions sent as the standard OAuth `scope`. Every other permission, such as
     * [YouVersionPermissions.HIGHLIGHTS], is one the authorization server treats as optional: it is requested
     * separately via the `requested_permissions` query parameter and may be declined without failing sign-in.
     */
    private val scopePermissions =
        setOf(
            YouVersionPermissions.OPENID,
            YouVersionPermissions.PROFILE,
            YouVersionPermissions.EMAIL,
            YouVersionPermissions.READ_NOTES,
            YouVersionPermissions.WRITE_NOTES,
        )

    // ----- User URLs
    fun authorizeUrl(
        appKey: String,
        permissions: Set<String>,
        redirectUri: String,
        parameters: SignInWithYouVersionPKCEParameters,
    ): String =
        buildYouVersionUrlString {
            path("/auth/authorize")

            parameter("response_type", "code")
            parameter("client_id", appKey)
            parameter("redirect_uri", redirectUri)
            parameter("nonce", parameters.nonce)
            parameter("state", parameters.state)
            parameter("code_challenge", parameters.codeChallenge)
            parameter("code_challenge_method", "S256")
            parameter("scope", scopeValue(permissions))
            requestedPermissionsValue(permissions)
                .takeIf { it.isNotEmpty() }
                ?.let { parameter("requested_permissions", it) }
            parameter("require_user_interaction", true)

            YouVersionPlatformConfiguration.installId?.let {
                parameter("x-yvp-installation-id", it)
            }
        }

    fun authTokenUrl(): String =
        buildYouVersionUrlString {
            path("/auth/token")
        }

    fun callbackUrl(): String =
        buildYouVersionUrlString {
            path("/auth/callback")
        }

    // ----- UserApi
    override fun signOut() {
        YouVersionPlatformConfiguration.clearAuthData()
    }

    override suspend fun userInfo(accessToken: String?): YouVersionUserInfo {
        TODO("Not yet implemented")
    }

    override suspend fun getSignInResult(
        callbackUri: String,
        state: String,
        codeVerifier: String,
        redirectUri: String,
        nonce: String,
    ): SignInWithYouVersionResult {
        val location =
            obtainLocation(
                callbackUri = callbackUri,
                state = state,
            )
        val code = obtainCode(location = location)
        val tokens =
            tokenRequest(
                code = code,
                codeVerifier = codeVerifier,
                redirectUri = redirectUri,
            )
        val result =
            extractSignInWithYouVersionResult(
                tokens = tokens,
                nonce = nonce,
                callbackUri = callbackUri,
            )
        return result
    }

    override suspend fun performRefresh(): SignInWithYouVersionResult {
        val refreshToken =
            YouVersionPlatformConfiguration.refreshToken
                ?: throw IllegalStateException("Refresh token not available. Cannot perform refresh.")
        val appKey =
            YouVersionPlatformConfiguration.appKey
                ?: throw IllegalStateException("App key not configured. Cannot perform refresh.")

        val tokens =
            httpClient
                .submitForm(
                    url = authTokenUrl(),
                    formParameters =
                        parameters {
                            append("grant_type", "refresh_token")
                            append("client_id", appKey)
                            append("refresh_token", refreshToken)
                        },
                ).body<RefreshTokenResponse>()

        val originalIdToken = YouVersionPlatformConfiguration.idToken

        return SignInWithYouVersionResult.create(
            accessToken = tokens.accessToken,
            expiresIn = tokens.expiresIn.toString(),
            refreshToken = tokens.refreshToken,
            idToken = originalIdToken,
            permissionValues = emptyList(),
            yvpUserId = null,
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun decodeJWT(token: String): Map<String, Any?> {
        return try {
            val segments = token.split('.')
            if (segments.size != 3) return emptyMap()

            val decodedBytes =
                Base64.UrlSafe
                    .withPadding(Base64.PaddingOption.ABSENT)
                    .decode(segments[1])
            val decodedString = decodedBytes.decodeToString()
            val jsonObject = Json.decodeFromString<JsonObject>(decodedString)

            jsonObject.mapValues { (_, jsonElement) ->
                jsonElement.jsonPrimitive.contentOrNull
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Exchanges an authorization code for an access token.
     *
     * @param code The authorization code received from the callback.
     * @param codeVerifier The original code verifier used in the initial authorization request.
     * @param redirectUri The original redirect URI used in the initial request.
     * @return A [TokenResponse] object with the parsed tokens and expiry info.
     */
    private suspend fun tokenRequest(
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): TokenResponse =
        httpClient
            .submitForm(
                url = authTokenUrl(),
                formParameters =
                    parameters {
                        append("grant_type", "authorization_code")
                        append("code", code)
                        append("redirect_uri", redirectUri)
                        append("client_id", YouVersionPlatformConfiguration.appKey ?: "")
                        append("code_verifier", codeVerifier)
                    },
            ).body()

    private suspend fun obtainLocation(
        callbackUri: String,
        state: String,
    ): String {
        val url = Url(callbackUri)

        if (url.parameters["state"] != state) {
            throw IllegalStateException("State mismatch")
        }

        val newUrl =
            URLBuilder()
                .apply {
                    takeFrom(Url(callbackUrl()))
                    parameters.appendAll(url.parameters)
                }.build()

        val client = PlatformCoreKoinComponent.httpClient.config { followRedirects = false }

        val response: HttpResponse = client.get(newUrl)

        if (response.status != HttpStatusCode.Found) { // HttpStatusCode.Found is 302
            throw Exception("Expected status 302 but got ${response.status.value}")
        }

        return response.headers["Location"]
            ?: throw Exception("Location header not found in 302 response")
    }

    private fun obtainCode(location: String): String {
        val locationUrl = Url(location)
        return locationUrl.parameters["code"]
            ?: throw Exception("Code not found in location URI")
    }

    private fun extractSignInWithYouVersionResult(
        tokens: TokenResponse,
        nonce: String,
        callbackUri: String,
    ): SignInWithYouVersionResult {
        val idClaims = decodeJWT(tokens.idToken)

        if (idClaims["nonce"] as? String != nonce) {
            throw IllegalStateException("Nonce mismatch. Potential replay attack.")
        }

        val grantedScopes = tokens.scope.split(" ", ",")
        val grantedPermissions = Url(callbackUri).parameters["granted_permissions"]?.split(",").orEmpty()
        val permissionValues = (grantedScopes + grantedPermissions).filter { it.isNotEmpty() }.distinct().sorted()

        return SignInWithYouVersionResult.create(
            accessToken = tokens.accessToken,
            expiresIn = tokens.expiresIn.toString(),
            refreshToken = tokens.refreshToken,
            idToken = tokens.idToken,
            permissionValues = permissionValues,
            yvpUserId = idClaims["sub"] as? String,
            name = idClaims["name"] as? String,
            profilePicture = idClaims["picture"] as? String,
            email = idClaims["email"] as? String,
        )
    }

    /**
     * Combines the scope permissions into a single, space-delimited scope string, ensuring "openid" is
     * included and excluding any optional permissions (which are sent via [requestedPermissionsValue]).
     */
    private fun scopeValue(permissions: Set<String>): String =
        (permissions + YouVersionPermissions.OPENID).intersect(scopePermissions).sorted().joinToString(" ")

    /**
     * Combines the requested optional permissions into a single, comma-delimited string, or an empty string
     * when none are requested.
     */
    private fun requestedPermissionsValue(permissions: Set<String>): String =
        permissions.subtract(scopePermissions).sorted().joinToString(",")
}
