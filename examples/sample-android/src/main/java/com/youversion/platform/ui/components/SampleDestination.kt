package com.youversion.platform.ui.components

import android.net.Uri

sealed class SampleDestination(
    val route: String,
) {
    data object Reader : SampleDestination("reader")

    data object Content : SampleDestination("content")

    data object ContentImages : SampleDestination("content/images")

    data object ContentImage : SampleDestination("content/image/{imageId}") {
        const val IMAGE_ID_ARGUMENT = "imageId"

        /** The route that opens the image with [imageId]. */
        fun route(imageId: String): String = "content/image/${Uri.encode(imageId)}"
    }

    data object Examples : SampleDestination("examples")

    data object ExamplesList : SampleDestination("examples/list")

    data object Navigate : SampleDestination("examples/navigate")

    data object Votd : SampleDestination("examples/votd")

    data object BibleCard : SampleDestination("examples/bible-card")

    data object Profile : SampleDestination("profile")
}
