package com.youversion.platform.core.users.model

import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.utilities.koin.YouVersionPlatformComponent
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object SignInWithYouVersion {
    val redirectURL = "youversionauth://callback".toUri()
    private val callbackUrL = "https://" + YouVersionPlatformConfiguration.apiHost + "/auth/callback"

    suspend fun obtainLocation(
        from: Uri,
        state: String,
    ): String {
        if (from.getQueryParameter("state") != state) {
            throw IllegalStateException("State mismatch")
        }

        val newUrl =
            callbackUrL
                .toUri()
                .buildUpon()
                .encodedQuery(from.encodedQuery) // Forward all original query params
                .build()

        val client = YouVersionPlatformComponent.httpClient.config { followRedirects = false }

        val response: HttpResponse = client.get(Url(newUrl.toString()))

        if (response.status != HttpStatusCode.Found) { // HttpStatusCode.Found is 302
            throw Exception("Expected status 302 but got ${response.status.value}")
        }

        return response.headers["Location"]
            ?: throw Exception("Location header not found in 302 response")
    }

    fun obtainCode(from: String): String {
        val locationUri = from.toUri()
        return locationUri.getQueryParameter("code")
            ?: throw Exception("Code not found in location URI")
    }

    suspend fun obtainTokens(
        from: String,
        codeVerifier: String,
    ): TokenResponse =
        SignInWithYouVersionPKCEAuthorizationRequestBuilder.tokenRequest(
            code = from,
            codeVerifier = codeVerifier,
            redirectUri = redirectURL.toString(),
        )

    fun extractSignInWithYouVersionResult(tokens: TokenResponse): SignInWithYouVersionResult {
        val idClaims = decodeJWT(tokens.idToken)
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

    val currentUserId: String?
        get() = currentIdClaims?.get("sub") as? String

    val currentUserName: String?
        get() = currentIdClaims?.get("name") as? String

    val currentUserEmail: String?
        get() = currentIdClaims?.get("email") as? String

    val currentUserProfilePicture: String?
        get() = currentIdClaims?.get("picture") as? String

    /**
     * A computed property that decodes and returns the claims from the currently stored ID token.
     *
     * @return A map of claims if an ID token exists and can be successfully decoded, otherwise `null`.
     */
    private val currentIdClaims: Map<String, Any?>?
        get() {
            val idToken = YouVersionPlatformConfiguration.idToken ?: return null
            return decodeJWT(idToken)
        }

    private fun decodeJWT(token: String): Map<String, Any?> {
        val segments = token.split(".")
        if (segments.size != 3) return emptyMap()
        var base64 = segments[1]
        base64 = base64.replace('-', '+').replace('_', '/')

        val padding =
            when (base64.length % 4) {
                2 -> "=="
                3 -> "="
                else -> ""
            }
        base64 += padding

        return try {
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            val decodedString = String(decodedBytes, Charsets.UTF_8)
            val jsonObject = Json.decodeFromString<JsonObject>(decodedString)

            jsonObject.mapValues { (_, jsonElement) ->
                jsonElement.jsonPrimitive.contentOrNull
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
