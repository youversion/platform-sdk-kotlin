package com.youversion.platform.core.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.bibles.api.BiblesApi
import com.youversion.platform.core.bibles.api.BiblesEndpoints
import com.youversion.platform.core.highlights.api.HighlightsApi
import com.youversion.platform.core.highlights.api.HighlightsEndpoints
import com.youversion.platform.core.languages.api.LanguagesApi
import com.youversion.platform.core.languages.api.LanguagesEndpoints
import com.youversion.platform.core.organizations.api.OrganizationsApi
import com.youversion.platform.core.organizations.api.OrganizationsEndpoints
import com.youversion.platform.core.users.api.UsersApi
import com.youversion.platform.core.users.api.UsersEndpoints
import com.youversion.platform.core.votd.api.VotdApi
import com.youversion.platform.core.votd.api.VotdEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

object YouVersionApi {
    val bible: BiblesApi = BiblesEndpoints
    val highlights: HighlightsApi = HighlightsEndpoints
    val languages: LanguagesApi = LanguagesEndpoints
    val organizations: OrganizationsApi = OrganizationsEndpoints
    val users: UsersApi = UsersEndpoints
    val votd: VotdApi = VotdEndpoints

    val isSignedIn: Boolean
        get() = YouVersionPlatformConfiguration.isSignedIn

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
