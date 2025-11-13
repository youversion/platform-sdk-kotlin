package com.youversion.platform.core.users.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.buildYouVersionUrl
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.users.model.SignInWithYouVersionResult
import com.youversion.platform.core.users.model.YouVersionUserInfo
import io.ktor.http.Url
import io.ktor.http.path

object UsersEndpoints : UsersApi {
    // ----- User URLs
    fun userUrl(accessToken: String): Url =
        buildYouVersionUrl {
            path("/auth/me")
            parameter("lat", accessToken)
        }

    fun authUrl(
        appKey: String,
        requiredPermissions: Set<SignInWithYouVersionPermission> = emptySet(),
        optionalPermissions: Set<SignInWithYouVersionPermission> = emptySet(),
    ) = buildYouVersionUrl {
        path("/auth/login")

        parameter("app_id", appKey)
        parameter("language", "en") // TODO load from the system
        parameter("required_perms", requiredPermissions.joinToString(",") { it.toString().lowercase() })
        parameter("opt_perms", optionalPermissions.joinToString(",") { it.toString().lowercase() })
        parameter("x-yvp-installation-id", YouVersionPlatformConfiguration.installId)
    }

    // ----- UserApi
    override suspend fun signIn(
        requiredPermissions: Set<SignInWithYouVersionPermission>,
        optionalPermissions: Set<SignInWithYouVersionPermission>,
    ): SignInWithYouVersionResult {
        TODO("Not yet implemented")
    }

    override fun signOut() {
        YouVersionPlatformConfiguration.setAccessToken(null)
    }

    override suspend fun userInfo(accessToken: String?): YouVersionUserInfo {
        TODO("Not yet implemented")
    }
}
