package com.youversion.platform.ui.components

sealed class SampleDestination(
    val route: String,
) {
    data object Reader : SampleDestination("reader")

    data object Votd : SampleDestination("votd")

    data object Widget : SampleDestination("widget")

    data object Profile : SampleDestination("profile")
}
