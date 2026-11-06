package com.youversion.platform.core.users.model

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class YouVersionUserInfo
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.FIRST_NAME) public val firstName: String? = null,
        @SerialName(CodingKey.LAST_NAME) public val lastName: String? = null,
        @SerialName(CodingKey.USER_ID) public val userId: String? = null,
        @SerialName(CodingKey.AVATAR_URL_FORMAT) public val avatarUrlFormat: String? = null,
    ) {
        private object CodingKey {
            const val FIRST_NAME = "first_name"
            const val LAST_NAME = "last_name"
            const val USER_ID = "id"
            const val AVATAR_URL_FORMAT = "avatar_url"
        }

        public val avatarUrl: String?
            get() {
                var urlString = avatarUrlFormat?.takeIf { it.isNotBlank() } ?: return null

                if (urlString.startsWith("//")) {
                    urlString = "https:$urlString"
                }
                urlString = urlString.replace("{width}", "200")
                urlString = urlString.replace("{height}", "200")
                return urlString
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is YouVersionUserInfo) return false
            return firstName == other.firstName &&
                lastName == other.lastName &&
                userId == other.userId &&
                avatarUrlFormat == other.avatarUrlFormat
        }

        override fun hashCode(): Int {
            var result = firstName.hashCode()
            result = 31 * result + lastName.hashCode()
            result = 31 * result + userId.hashCode()
            result = 31 * result + avatarUrlFormat.hashCode()
            return result
        }

        override fun toString(): String =
            "YouVersionUserInfo(firstName=$firstName, lastName=$lastName, userId=$userId, " +
                "avatarUrlFormat=$avatarUrlFormat)"

        public companion object {
            public val preview: YouVersionUserInfo
                get() =
                    YouVersionUserInfo(
                        firstName = "John",
                        lastName = "Smith",
                        userId = "12345",
                        avatarUrlFormat = null,
                    )
        }
    }
