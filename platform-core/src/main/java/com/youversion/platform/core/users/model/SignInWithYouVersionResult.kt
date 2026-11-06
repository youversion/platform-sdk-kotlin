package com.youversion.platform.core.users.model

import com.youversion.platform.core.di.PlatformInternalApi
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
public class SignInWithYouVersionResult
    @PlatformInternalApi
    constructor(
        public val accessToken: String?,
        public val expiryDate: Date?,
        public val refreshToken: String?,
        public val idToken: String?,
        public val permissions: List<SignInWithYouVersionPermission>,
        public val yvpUserId: String?,
        public val name: String?,
        public val profilePicture: String?,
        public val email: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SignInWithYouVersionResult) return false
            return accessToken == other.accessToken &&
                expiryDate == other.expiryDate &&
                refreshToken == other.refreshToken &&
                idToken == other.idToken &&
                permissions == other.permissions &&
                yvpUserId == other.yvpUserId &&
                name == other.name &&
                profilePicture == other.profilePicture &&
                email == other.email
        }

        override fun hashCode(): Int {
            var result = accessToken.hashCode()
            result = 31 * result + expiryDate.hashCode()
            result = 31 * result + refreshToken.hashCode()
            result = 31 * result + idToken.hashCode()
            result = 31 * result + permissions.hashCode()
            result = 31 * result + yvpUserId.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + profilePicture.hashCode()
            result = 31 * result + email.hashCode()
            return result
        }

        override fun toString(): String =
            "SignInWithYouVersionResult(accessToken=${accessToken.redacted()}, expiryDate=$expiryDate, " +
                "refreshToken=${refreshToken.redacted()}, idToken=${idToken.redacted()}, permissions=$permissions, " +
                "yvpUserId=$yvpUserId, name=$name, profilePicture=$profilePicture, email=${email.redacted()})"

        private fun String?.redacted(): String? = this?.let { "<redacted>" }

        public companion object {
            /**
             * Factory function to create a [SignInWithYouVersionResult] while calculating the expiry date
             * from an "expires_in" seconds string.
             *
             * @param expiresIn The number of seconds from now until the token expires. Defaults to "0" if null.
             */
            public fun create(
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
