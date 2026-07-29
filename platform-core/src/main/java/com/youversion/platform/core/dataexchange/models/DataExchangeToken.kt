package com.youversion.platform.core.dataexchange.models

import kotlinx.serialization.Serializable

/**
 * A short-lived token authorizing one run of the data exchange permission flow.
 *
 * @property token The token to pass to the permission page.
 */
@Serializable
data class DataExchangeToken(
    val token: String,
)
