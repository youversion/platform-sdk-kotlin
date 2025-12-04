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

object YouVersionApi {
    val bible: BiblesApi = BiblesEndpoints
    val highlights: HighlightsApi = HighlightsEndpoints
    val languages: LanguagesApi = LanguagesEndpoints
    val organizations: OrganizationsApi = OrganizationsEndpoints
    val users: UsersApi = UsersEndpoints
    val votd: VotdApi = VotdEndpoints

    val isSignedIn: Boolean
        get() = YouVersionPlatformConfiguration.accessToken != null
}
