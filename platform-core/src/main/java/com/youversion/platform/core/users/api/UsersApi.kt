package com.youversion.platform.core.users.api

import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.users.model.SignInWithYouVersionResult
import com.youversion.platform.core.users.model.YouVersionUserInfo

interface UsersApi {
    fun signOut()

    suspend fun userInfo(accessToken: String?): YouVersionUserInfo
}
