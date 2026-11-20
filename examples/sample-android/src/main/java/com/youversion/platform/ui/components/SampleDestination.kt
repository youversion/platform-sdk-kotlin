package com.youversion.platform.ui.components

sealed class SampleDestination(
    val route: String,
) {
    data object Reader : SampleDestination("reader")

    data object Content : SampleDestination("content")

    data object Examples : SampleDestination("examples")

    data object ExamplesList : SampleDestination("examples/list")

    data object Navigate : SampleDestination("examples/navigate")

    data object Votd : SampleDestination("examples/votd")

    data object BibleCard : SampleDestination("examples/bible-card")

    data object Profile : SampleDestination("profile")
}
