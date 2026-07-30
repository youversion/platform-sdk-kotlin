package com.youversion.platform.core.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.bibles.api.BiblesApi
import com.youversion.platform.core.bibles.api.BiblesEndpoints
import com.youversion.platform.core.dataexchange.api.DataExchangeApi
import com.youversion.platform.core.dataexchange.api.DataExchangeEndpoints
import com.youversion.platform.core.highlights.api.HighlightsApi
import com.youversion.platform.core.highlights.api.HighlightsEndpoints
import com.youversion.platform.core.languages.api.LanguagesApi
import com.youversion.platform.core.languages.api.LanguagesEndpoints
import com.youversion.platform.core.organizations.api.OrganizationsApi
import com.youversion.platform.core.organizations.api.OrganizationsEndpoints
import com.youversion.platform.core.users.api.UsersApi
import com.youversion.platform.core.users.api.UsersEndpoints
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.votd.api.VotdApi
import com.youversion.platform.core.votd.api.VotdEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Date

object YouVersionApi {
    val bible: BiblesApi = BiblesEndpoints
    val dataExchange: DataExchangeApi = DataExchangeEndpoints
    val highlights: HighlightsApi = HighlightsEndpoints
    val languages: LanguagesApi = LanguagesEndpoints
    val organizations: OrganizationsApi = OrganizationsEndpoints
    val users: UsersApi = UsersEndpoints
    val votd: VotdApi = VotdEndpoints

    val isSignedIn: Boolean
        get() = YouVersionPlatformConfiguration.isSignedIn

    /**
     * Identifies the session the SDK is signed in to, or `null` when signed out. The value is opaque and only
     * meaningful compared against another read of this property.
     *
     * Bind stored state to this rather than to [UsersApi.currentUserId] when it must not carry across a change of
     * user: an account is nameable only when an ID token is present, so two sessions for different users can both
     * report no account. Those fall back to a token digest, taken over the refresh token where one exists so that
     * refreshing an access token does not read as a new user.
     */
    val currentSessionId: String?
        get() =
            sessionId(
                accessToken = YouVersionPlatformConfiguration.accessToken,
                refreshToken = YouVersionPlatformConfiguration.refreshToken,
                idToken = YouVersionPlatformConfiguration.idToken,
            )

    /**
     * Identifies the session the given tokens belong to, by the same rules as [currentSessionId], without requiring
     * them to be the ones the SDK is configured with. This allows incoming tokens to be compared against the current
     * ones before they are stored.
     */
    internal fun sessionId(
        accessToken: String?,
        refreshToken: String?,
        idToken: String?,
    ): String? {
        if (accessToken == null) return null
        idToken?.let { users.decodeJWT(it)["sub"] as? String }?.let { return "user:$it" }
        return "token:${(refreshToken ?: accessToken).sha256()}"
    }

    /**
     * Whether the signed-in user has granted the given permission to this app.
     *
     * @param permission The permission to check.
     * @return `true` if the user has granted the permission, `false` otherwise.
     */
    fun hasPermission(permission: SignInWithYouVersionPermission): Boolean =
        YouVersionPlatformConfiguration.grantedPermissions.contains(permission)

    /**
     * Checks if the current access token is valid. If the token is expired or close to
     * expiring, it will attempt to refresh it using the stored refresh token.
     *
     * @return `true` if a valid token exists or was successfully refreshed, `false` otherwise.
     */
    suspend fun hasValidToken(): Boolean {
        val expiryDate = YouVersionPlatformConfiguration.expiryDate ?: return false

        val thirtySecondsFromNow = Date(System.currentTimeMillis() + 30_000L)
        if (expiryDate.after(thirtySecondsFromNow)) {
            return true
        }

        return try {
            val result = users.performRefresh()

            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                idToken = result.idToken,
                expiryDate = result.expiryDate,
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}

private fun String.sha256(): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }
