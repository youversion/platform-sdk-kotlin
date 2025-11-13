package com.youversion.platform.core.users.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouVersionUserInfo(
    @SerialName(CodingKey.FIRST_NAME) val firstName: String? = null,
    @SerialName(CodingKey.LAST_NAME) val lastName: String? = null,
    @SerialName(CodingKey.USER_ID) val userId: String? = null,
    @SerialName(CodingKey.AVATAR_URL_FORMAT) val avatarUrlFormat: String? = null,
) {
    private object CodingKey {
        const val FIRST_NAME = "first_name"
        const val LAST_NAME = "last_name"
        const val USER_ID = "id"
        const val AVATAR_URL_FORMAT = "avatar_url"
    }

    val avatarUrl: String?
        get() {
            var urlString = avatarUrlFormat ?: return null

            if (urlString.startsWith("//")) {
                urlString = "https:$urlString"
            }
            urlString = urlString.replace("{width}", "200")
            urlString = urlString.replace("{height}", "200")
            return urlString
        }

    companion object {
        val preview: YouVersionUserInfo
            get() =
                YouVersionUserInfo(
                    firstName = "John",
                    lastName = "Smith",
                    userId = "12345",
                    avatarUrlFormat = null,
                )
    }
}
