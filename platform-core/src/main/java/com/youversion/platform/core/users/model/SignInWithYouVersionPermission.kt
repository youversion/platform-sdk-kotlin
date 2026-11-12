package com.youversion.platform.core.users.model

public enum class SignInWithYouVersionPermission(
    public val rawValue: String,
) {
    OPENID("openid"),
    PROFILE("profile"),
    EMAIL("email"),
    HIGHLIGHTS("highlights"),
    ;

    public companion object {
        /**
         * The permission matching [rawValue], or `null` when no permission matches.
         *
         * Values the SDK does not recognize resolve to `null` rather than failing, so a permission
         * introduced server-side does not break older clients.
         *
         * @param rawValue The raw value to resolve.
         */
        public fun fromRawValue(rawValue: String): SignInWithYouVersionPermission? =
            entries.find {
                it.rawValue ==
                    rawValue
            }
    }
}
