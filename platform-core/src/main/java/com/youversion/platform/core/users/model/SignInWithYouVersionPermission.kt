package com.youversion.platform.core.users.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class SignInWithYouVersionPermission(
    val rawValue: String,
) {
    BIBLES("bibles"),

    HIGHLIGHTS("highlights"),

    VOTD("votd"),

    DEMOGRAPHICS("demographics"),

    BIBLE_ACTIVITY("bible_activity"),
}
