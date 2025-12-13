package com.youversion.platform.core.users.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.users.model.SignInWithYouVersionPKCEParameters
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.users.model.SignInWithYouVersionResult
import com.youversion.platform.core.users.model.TokenResponse
import com.youversion.platform.core.users.model.YouVersionUserInfo
import com.youversion.platform.core.utilities.koin.YouVersionPlatformComponent
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

object UsersEndpoints : UsersApi {
    private val httpClient: HttpClient
        get() = YouVersionPlatformComponent.httpClient

    // ----- User URLs
    fun userUrl(accessToken: String): String =
        buildYouVersionUrlString {
            path("/auth/me")
            parameter("lat", accessToken)
        }

    fun authorizeUrl(
        appKey: String,
        permissions: Set<SignInWithYouVersionPermission>,
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
        val result = extractSignInWithYouVersionResult(tokens = tokens, nonce = nonce)
        return result
    }

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

        val client = YouVersionPlatformComponent.httpClient.config { followRedirects = false }

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
    ): SignInWithYouVersionResult {
        val idClaims = decodeJWT(tokens.idToken)

        if (idClaims["nonce"] as? String != nonce) {
            throw IllegalStateException("Nonce mismatch. Potential replay attack.")
        }

        val permissions =
            tokens.scope
                .split(",")
                .mapNotNull { rawValue ->
                    SignInWithYouVersionPermission.entries.find { it.rawValue == rawValue }
                }.toSet()

        return SignInWithYouVersionResult.create(
            accessToken = tokens.accessToken,
            expiresIn = tokens.expiresIn.toString(),
            refreshToken = tokens.refreshToken,
            idToken = tokens.idToken,
            permissions = permissions.toList(),
            yvpUserId = idClaims["sub"] as? String,
            name = idClaims["name"] as? String,
            profilePicture = idClaims["picture"] as? String,
            email = idClaims["email"] as? String,
        )
    }

    /**
     * Combines a set of permissions into a single, space-delimited scope string, ensuring "openid" is included.
     */
    private fun scopeValue(permissions: Set<SignInWithYouVersionPermission>): String {
        val fullScopes = permissions.union(setOf(SignInWithYouVersionPermission.OPENID))
        return fullScopes.map { it.rawValue }.sorted().joinToString(" ")
    }
}
