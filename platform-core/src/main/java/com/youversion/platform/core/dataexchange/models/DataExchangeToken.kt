package com.youversion.platform.core.dataexchange.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.Serializable

/**
 * A short-lived token authorizing one run of the data exchange permission flow.
 *
 * @property token The token to pass to the permission page.
 */
@Serializable
public class DataExchangeToken
    @PlatformInternalApi
    constructor(
        public val token: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DataExchangeToken) return false
            return token == other.token
        }

        override fun hashCode(): Int = token.hashCode()

        override fun toString(): String = "DataExchangeToken(token=<redacted>)"
    }
