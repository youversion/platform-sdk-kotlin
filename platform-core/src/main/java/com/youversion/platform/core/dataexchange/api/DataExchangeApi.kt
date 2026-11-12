package com.youversion.platform.core.dataexchange.api

import com.youversion.platform.core.dataexchange.models.DataExchangeToken
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission

public interface DataExchangeApi {
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
     *         user is signed in, the app key is not configured, or the server does not accept the user's access
     *         token (`401`),
     *         [com.youversion.platform.core.api.YouVersionNetworkException.Reason.NOT_PERMITTED] when the server
     *         refuses the request (`403`),
     *         [com.youversion.platform.core.api.YouVersionNetworkException.Reason.RATE_LIMITED] for `429`,
     *         [com.youversion.platform.core.api.YouVersionNetworkException.Reason.SERVER_ERROR] for `5xx`,
     *         [com.youversion.platform.core.api.YouVersionNetworkException.Reason.REQUEST_REJECTED] for another
     *         `4xx`, or [com.youversion.platform.core.api.YouVersionNetworkException.Reason.REQUEST_FAILED]
     *         otherwise. The exception's `statusCode`, `serverMessage`, `retryAfter` and `validationIssues` carry
     *         the server's details.
     */
    public suspend fun dataExchangeToken(permissions: Set<SignInWithYouVersionPermission>): DataExchangeToken
}
