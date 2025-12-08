package com.youversion.platform.core.users.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.users.model.SignInWithYouVersionResult
import com.youversion.platform.core.users.model.YouVersionUserInfo

interface UsersApi {
    fun signOut()

    suspend fun userInfo(accessToken: String?): YouVersionUserInfo

    suspend fun getSignInResult(
        callbackUri: String,
        state: String,
        codeVerifier: String,
        redirectUri: String,
        nonce: String,
    ): SignInWithYouVersionResult

    fun decodeJWT(token: String): Map<String, Any?>

    val currentUserId: String?
        get() = currentIdClaims?.get("sub") as? String

    val currentUserName: String?
        get() = currentIdClaims?.get("name") as? String

    val currentUserEmail: String?
        get() = currentIdClaims?.get("email") as? String

    val currentUserProfilePicture: String?
        get() = currentIdClaims?.get("picture") as? String

    /**
     * A computed property that decodes and returns the claims from the currently stored ID token.
     *
     * @return A map of claims if an ID token exists and can be successfully decoded, otherwise `null`.
     */
    private val currentIdClaims: Map<String, Any?>?
        get() {
            val idToken = YouVersionPlatformConfiguration.idToken ?: return null
            return decodeJWT(idToken)
        }
}
