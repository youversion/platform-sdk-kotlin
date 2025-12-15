package com.youversion.platform.core.users.model

import com.youversion.platform.core.users.api.UsersEndpoints
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.encoding.Base64

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
    val url: String,
    val parameters: SignInWithYouVersionPKCEParameters,
) {
    companion object {
        /**
         * Creates a fully-formed PKCE authorization request.
         *
         * @param appKey The application's unique key (client_id).
         * @param permissions The set of permissions (scopes) being requested.
         * @param redirectUri The callback URL where the authorization code will be sent.
         * @return A [SignInWithYouVersionPKCEAuthorizationRequest] containing the URL and parameters.
         * @throws SignInWithYouVersionPKCEAuthorizationError if random string generation or URL construction fails.
         */
        operator fun invoke(
            appKey: String,
            permissions: Set<SignInWithYouVersionPermission>,
            redirectUri: String,
        ): SignInWithYouVersionPKCEAuthorizationRequest {
            val codeVerifier = randomURLSafeString(32)
            val codeChallenge = codeChallenge(verifier = codeVerifier)
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
                UsersEndpoints
                    .authorizeUrl(
                        appKey = appKey,
                        permissions = permissions,
                        redirectUri = redirectUri,
                        parameters = parameters,
                    )

            return SignInWithYouVersionPKCEAuthorizationRequest(url = url, parameters = parameters)
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
         * Hashes the verifier using SHA-256 and encodes it in Base64-URL-safe format.
         */
        private fun codeChallenge(verifier: String): String {
            val bytes = verifier.toByteArray(Charsets.UTF_8)
            val messageDigest = MessageDigest.getInstance("SHA-256")
            val digest = messageDigest.digest(bytes)
            return base64URLEncodedString(digest)
        }

        /**
         * Encodes a byte array into a Base64 string safe for use in URLs.
         */
        private fun base64URLEncodedString(data: ByteArray): String =
            Base64.UrlSafe
                .withPadding(Base64.PaddingOption.ABSENT)
                .encode(data)
    }
}

/**
 * Sealed class for custom errors related to the PKCE authorization flow.
 */
sealed class SignInWithYouVersionPKCEAuthorizationError : Throwable() {
    class RandomGenerationFailed : SignInWithYouVersionPKCEAuthorizationError()
}
