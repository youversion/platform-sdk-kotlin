package com.youversion.platform.core.users.model

import android.net.Uri
import android.util.Base64
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.utilities.koin.YouVersionPlatformComponent
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Data class holding the parameters required for a PKCE authorization flow.
 *
 * @property codeVerifier The random, high-entropy string generated for the flow.
 * @property codeChallenge The Base64-URL-encoded SHA-256 hash of the codeVerifier.
 * @property state An opaque value used to prevent cross-site request forgery.
 * @property nonce An opaque value used to associate a client session with an ID token.
 */
data class SignInWithYouVersionPKCEParameters(
    val codeVerifier: String,
    val codeChallenge: String,
    val state: String,
    val nonce: String,
)

/**
 * Data class holding the generated authorization URL and the PKCE parameters used to create it.
 */
data class SignInWithYouVersionPKCEAuthorizationRequest(
    val url: Uri,
    val parameters: SignInWithYouVersionPKCEParameters,
)

/**
 * Represents the JSON response from the /auth/token endpoint.
 */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("scope") val scope: String,
    @SerialName("token_type") val tokenType: String,
)

/**
 * Sealed class for custom errors related to the PKCE authorization flow.
 */
sealed class SignInWithYouVersionPKCEAuthorizationError : Throwable() {
    class RandomGenerationFailed : SignInWithYouVersionPKCEAuthorizationError()
}

/**
 * Builder object for creating PKCE authorization requests and token exchange requests.
 */
object SignInWithYouVersionPKCEAuthorizationRequestBuilder {
    /**
     * Creates a fully-formed PKCE authorization request.
     *
     * @param appKey The application's unique key (client_id).
     * @param permissions The set of permissions (scopes) being requested.
     * @param redirectUri The callback URL where the authorization code will be sent.
     * @return A [SignInWithYouVersionPKCEAuthorizationRequest] containing the URL and parameters.
     * @throws SignInWithYouVersionPKCEAuthorizationError if random string generation or URL construction fails.
     */
    fun make(
        appKey: String,
        permissions: Set<SignInWithYouVersionPermission>,
        redirectUri: Uri,
    ): SignInWithYouVersionPKCEAuthorizationRequest {
        val codeVerifier = randomURLSafeString(32)
        val codeChallenge = codeChallenge(forVerifier = codeVerifier)
        val state = randomURLSafeString(24)
        val nonce = randomURLSafeString(24)

        val parameters =
            SignInWithYouVersionPKCEParameters(
                codeVerifier = codeVerifier,
                codeChallenge = codeChallenge,
                state = state,
                nonce = nonce,
            )

        val url =
            authorizeURL(
                appKey = appKey,
                permissions = permissions,
                redirectUri = redirectUri,
                parameters = parameters,
            )

        return SignInWithYouVersionPKCEAuthorizationRequest(url = url, parameters = parameters)
    }

    /**
     * Exchanges an authorization code for an access token.
     *
     * @param code The authorization code received from the callback.
     * @param codeVerifier The original code verifier used in the initial authorization request.
     * @param redirectUri The original redirect URI used in the initial request.
     * @return A [TokenResponse] object with the parsed tokens and expiry info.
     */
    suspend fun tokenRequest(
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): TokenResponse {
        val url = "https://${YouVersionPlatformConfiguration.apiHost}/auth/token"
        val httpClient = YouVersionPlatformComponent.httpClient

        return httpClient
            .submitForm(
                url = url,
                formParameters =
                    Parameters.build {
                        append("grant_type", "authorization_code")
                        append("code", code)
                        append("redirect_uri", redirectUri)
                        append("client_id", YouVersionPlatformConfiguration.appKey ?: "")
                        append("code_verifier", codeVerifier)
                    },
            ).body()
    }

    private fun authorizeURL(
        appKey: String,
        permissions: Set<SignInWithYouVersionPermission>,
        redirectUri: Uri,
        parameters: SignInWithYouVersionPKCEParameters,
    ): Uri {
        val builder =
            Uri
                .Builder()
                .scheme("https")
                .authority(YouVersionPlatformConfiguration.apiHost)
                .path("/auth/authorize")
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", appKey)
                .appendQueryParameter("redirect_uri", redirectUri.toString())
                .appendQueryParameter("nonce", parameters.nonce)
                .appendQueryParameter("state", parameters.state)
                .appendQueryParameter("code_challenge", parameters.codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")

        scopeValue(permissions).let {
            builder.appendQueryParameter("scope", it)
        }

        YouVersionPlatformConfiguration.installId?.let {
            builder.appendQueryParameter("x-yvp-installation-id", it)
        }

        return builder.build()
    }

    /**
     * Hashes the verifier using SHA-256 and encodes it in Base64-URL-safe format.
     */
    private fun codeChallenge(forVerifier: String): String {
        val bytes = forVerifier.toByteArray(Charsets.UTF_8)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return base64URLEncodedString(digest)
    }

    /**
     * Generates a cryptographically secure random string.
     * @throws SignInWithYouVersionPKCEAuthorizationError if random generation fails.
     */
    private fun randomURLSafeString(byteCount: Int): String =
        try {
            val random = SecureRandom()
            val bytes = ByteArray(byteCount)
            random.nextBytes(bytes)
            base64URLEncodedString(bytes)
        } catch (_: Exception) {
            throw SignInWithYouVersionPKCEAuthorizationError.RandomGenerationFailed()
        }

    /**
     * Encodes a byte array into a Base64 string safe for use in URLs.
     */
    private fun base64URLEncodedString(data: ByteArray): String =
        Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    /**
     * Combines a set of permissions into a single, space-delimited scope string, ensuring "openid" is included.
     */
    private fun scopeValue(permissions: Set<SignInWithYouVersionPermission>): String {
        val permissionValues = permissions.map { it.rawValue }.toMutableSet()
        permissionValues.add("openid")
        return permissionValues.sorted().joinToString(" ")
    }
}
