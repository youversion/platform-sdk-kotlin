package com.youversion.platform.core.users.model

enum class SignInWithYouVersionPermission(
    val rawValue: String,
) {
    OPENID("openid"),
    PROFILE("profile"),
    EMAIL("email"),
}
