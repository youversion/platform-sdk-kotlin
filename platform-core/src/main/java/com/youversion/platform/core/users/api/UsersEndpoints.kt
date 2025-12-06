package com.youversion.platform.core.users.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.users.model.YouVersionUserInfo
import io.ktor.http.path

object UsersEndpoints : UsersApi {
    // ----- User URLs
    fun userUrl(accessToken: String): String =
        buildYouVersionUrlString {
            path("/auth/me")
            parameter("lat", accessToken)
        }

    fun authUrl(
        appKey: String,
        requiredPermissions: Set<SignInWithYouVersionPermission> = emptySet(),
        optionalPermissions: Set<SignInWithYouVersionPermission> = emptySet(),
    ): String =
        buildYouVersionUrlString {
            path("/auth/login")

            parameter("app_id", appKey)
            parameter("language", "en") // TODO load from the system
            parameter("required_perms", requiredPermissions.joinToString(",") { it.toString().lowercase() })
            parameter("opt_perms", optionalPermissions.joinToString(",") { it.toString().lowercase() })
            parameter("x-yvp-installation-id", YouVersionPlatformConfiguration.installId)
        }

    // ----- UserApi
    override fun signOut() {
        YouVersionPlatformConfiguration.clearAuthData()
    }

    override suspend fun userInfo(accessToken: String?): YouVersionUserInfo {
        TODO("Not yet implemented")
    }
}
