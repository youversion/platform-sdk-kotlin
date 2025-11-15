package com.youversion.platform.core.users.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SignInWithYouVersionPermission {
    @SerialName("bibles")
    BIBLES,

    @SerialName("highlights")
    HIGHLIGHTS,

    @SerialName("votd")
    VOTD,

    @SerialName("demographics")
    DEMOGRAPHICS,

    @SerialName("bible_activity")
    BIBLE_ACTIVITY,
}
