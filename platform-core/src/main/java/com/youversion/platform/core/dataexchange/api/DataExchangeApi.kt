package com.youversion.platform.core.dataexchange.api

import com.youversion.platform.core.dataexchange.models.DataExchangeToken
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission

interface DataExchangeApi {
    /**
     * Creates a short-lived token authorizing one run of the data exchange permission flow.
     *
     * A signed-in user can grant a permission this way without repeating the whole sign-in, which is what makes it
     * possible to ask for a permission at the moment it is needed.
     *
     * @param permissions The permissions to ask the signed-in user to grant.
     * @return The token to pass to the permission page.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] with reason
     *         [com.youversion.platform.core.api.YouVersionNetworkException.Reason.MISSING_AUTHENTICATION] when no
     *         user is signed in or the app key is not configured,
     *         [com.youversion.platform.core.api.YouVersionNetworkException.Reason.NOT_PERMITTED] when the server
     *         refuses the request, or
     *         [com.youversion.platform.core.api.YouVersionNetworkException.Reason.CANNOT_DOWNLOAD] otherwise.
     */
    suspend fun dataExchangeToken(permissions: Set<SignInWithYouVersionPermission>): DataExchangeToken
}
