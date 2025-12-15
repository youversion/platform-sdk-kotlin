package com.youversion.platform.core.users.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the JSON response from the /auth/token endpoint.
 */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("scope") val scope: String,
    @SerialName("token_type") val tokenType: String,
)
