package com.youversion.platform.core.users.model

import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Represents the result of a successful sign-in flow with YouVersion.
 *
 * @property accessToken The OAuth access token for making authenticated API requests. Can be null if authentication failed at the final step.
 * @property expiryDate The calculated date and time when the access token will expire. Can be null if expiry information is not provided.
 * @property refreshToken The token used to refresh the access token once it expires.
 * @property idToken A JSON Web Token (JWT) that contains the user's identity and profile information, proving that the user has been authenticated.
 * @property permissions A list of permissions granted by the user.
 * @property yvpUserId The unique YouVersion user ID.
 * @property name The user's name, if available and permission was granted.
 * @property profilePicture A URL to the user's profile picture, if available and permission was granted.
 * @property email The user's email address, if available and permission was granted.
 */
data class SignInWithYouVersionResult(
    val accessToken: String?,
    val expiryDate: Date?,
    val refreshToken: String?,
    val idToken: String?,
    val permissions: List<SignInWithYouVersionPermission>,
    val yvpUserId: String?,
    val name: String?,
    val profilePicture: String?,
    val email: String?,
) {
    companion object {
        /**
         * Factory function to create a [SignInWithYouVersionResult] while calculating the expiry date
         * from an "expires_in" seconds string.
         *
         * @param expiresIn The number of seconds from now until the token expires. Defaults to "0" if null.
         */
        fun create(
            accessToken: String?,
            expiresIn: String?,
            refreshToken: String?,
            idToken: String?,
            permissions: List<SignInWithYouVersionPermission>,
            yvpUserId: String?,
            name: String? = null,
            profilePicture: String? = null,
            email: String? = null,
        ): SignInWithYouVersionResult {
            val expiresInSeconds = expiresIn?.toLongOrNull() ?: 0L
            val expiryDate =
                if (expiresInSeconds > 0) {
                    Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expiresInSeconds))
                } else {
                    null
                }

            return SignInWithYouVersionResult(
                accessToken = accessToken,
                expiryDate = expiryDate,
                refreshToken = refreshToken,
                idToken = idToken,
                permissions = permissions,
                yvpUserId = yvpUserId,
                name = name,
                profilePicture = profilePicture,
                email = email,
            )
        }
    }
}
