package com.youversion.platform.reader.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The Save action's bookmark, the Swift SDK's `YVBookmark` asset. */
internal val BookmarkIcon: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector
        .Builder(
            name = "BookmarkIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData =
                addPathNodes(
                    "M15 3C16.66 3 18.557 2.901 19.828 4.172C21.099 5.443 21 7.339 21 9V17.407C21 17.885 21.005 " +
                        "18.472 20.934 18.954C20.858 19.465 20.624 20.39 19.693 20.964C18.763 21.538 17.831 21.331 " +
                        "17.341 21.17C16.878 21.018 16.356 20.749 15.929 20.534L12.924 19.024C12.455 18.789 12.217 " +
                        "18.672 12.04 18.602C12.034 18.599 12.028 18.598 12.022 18.601C11.844 18.67 11.606 18.788 " +
                        "11.136 19.022L8.059 20.552C7.633 20.764 7.111 21.03 6.648 21.18C6.158 21.339 5.229 21.541 " +
                        "4.302 20.967C3.375 20.393 3.142 19.471 3.066 18.961C2.995 18.48 3 17.894 3 17.418V9C3 " +
                        "7.339 2.901 5.443 4.172 4.172C5.443 2.901 7.339 3 9 3H15ZM9 5C7.114 5 6.172 5 5.586 " +
                        "5.586C5 6.172 5 7.114 5 9V17.418C5 18.504 5 19.047 5.355 19.267C5.71 19.486 6.196 19.244 " +
                        "7.168 18.761L10.244 17.231C11.123 16.793 11.562 16.575 12.034 16.576C12.506 16.577 12.945 " +
                        "16.797 13.822 17.237L16.826 18.747C17.8 19.236 18.287 19.481 18.644 19.262C19 19.042 19 " +
                        "18.497 19 17.407V9C19 7.114 19 6.172 18.414 5.586C17.828 5 16.886 5 15 5H9Z",
                ),
            fill = SolidColor(Color(0xFF121212)),
        ).build()
}
