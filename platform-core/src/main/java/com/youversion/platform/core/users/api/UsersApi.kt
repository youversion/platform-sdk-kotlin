package com.youversion.platform.core.users.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.users.model.SignInWithYouVersionResult
import com.youversion.platform.core.users.model.YouVersionUserInfo

public interface UsersApi {
    public fun signOut()

    public suspend fun userInfo(accessToken: String?): YouVersionUserInfo

    public suspend fun getSignInResult(
        callbackUri: String,
        state: String,
        codeVerifier: String,
        redirectUri: String,
        nonce: String,
    ): SignInWithYouVersionResult

    public suspend fun performRefresh(): SignInWithYouVersionResult

    public fun decodeJWT(token: String): Map<String, Any?>

    public val currentUserId: String?
        get() = currentIdClaims?.get("sub") as? String

    public val currentUserName: String?
        get() = currentIdClaims?.get("name") as? String

    public val currentUserEmail: String?
        get() = currentIdClaims?.get("email") as? String

    public val currentUserProfilePicture: String?
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
