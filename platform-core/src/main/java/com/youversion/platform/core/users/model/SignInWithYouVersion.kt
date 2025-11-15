package com.youversion.platform.core.users.model

import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import com.youversion.platform.core.utilities.koin.YouVersionPlatformComponent
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.serialization.json.Json

object SignInWithYouVersion {
    val redirectURL = "youversionauth://callback".toUri()
    private val callbackUrL = "https://api-staging.youversion.com/auth/callback"

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

    fun extractSignInWithYouVersionResult(from: TokenResponse): SignInWithYouVersionResult {
        val idClaims = decodeJWT(from.idToken)
        val permissions =
            from.scope
                .split(" ")
                .mapNotNull { rawValue ->
                    SignInWithYouVersionPermission.entries.find { it.rawValue == rawValue }
                }.toSet()

        return SignInWithYouVersionResult.create(
            accessToken = from.accessToken,
            expiresIn = from.expiresIn.toString(),
            refreshToken = from.refreshToken,
            permissions = permissions.toList(),
            yvpUserId = idClaims["sub"] as? String,
            name = idClaims["name"] as? String,
            profilePicture = idClaims["picture"] as? String,
            email = idClaims["email"] as? String,
        )
    }

    private fun decodeJWT(token: String): Map<String, Any> {
        val segments = token.split(".")
        if (segments.size < 2) return emptyMap()

        val payload = segments[1]
        val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
        val decodedString = String(decodedBytes, Charsets.UTF_8)

        return Json.decodeFromString<Map<String, Any>>(decodedString)
    }
}
