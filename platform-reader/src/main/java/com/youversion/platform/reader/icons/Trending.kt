package com.youversion.platform.reader.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Trending: ImageVector
    get() {
        if (_Trending != null) {
            return _Trending!!
        }
        _Trending =
            ImageVector
                .Builder(
                    name = "Trending",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                ).apply {
                    path(
                        fill = SolidColor(Color(0xFF121212)),
                        pathFillType = PathFillType.EvenOdd,
                    ) {
                        moveTo(15.929f, 11.795f)
                        lineTo(15.93f, 11.798f)
                        curveTo(16.541f, 12.891f, 16.731f, 14.213f, 16.36f, 15.516f)
                        curveTo(15.679f, 17.904f, 13.188f, 19.291f, 10.799f, 18.611f)
                        curveTo(8.563f, 17.974f, 7.206f, 15.752f, 7.599f, 13.511f)
                        curveTo(8.592f, 14.446f, 10.113f, 14.675f, 11.368f, 13.977f)
                        curveTo(12.073f, 13.585f, 12.641f, 12.953f, 12.866f, 12.077f)
                        curveTo(13.059f, 11.322f, 12.938f, 10.601f, 12.806f, 10.081f)
                        curveTo(12.347f, 8.269f, 12.169f, 7.18f, 12.363f, 6.319f)
                        curveTo(12.422f, 6.058f, 12.528f, 5.77f, 12.743f, 5.457f)
                        curveTo(12.897f, 6.429f, 13.194f, 7.267f, 13.606f, 8.065f)
                        curveTo(13.949f, 8.728f, 14.36f, 9.34f, 14.73f, 9.89f)
                        curveTo(14.742f, 9.907f, 14.753f, 9.924f, 14.764f, 9.941f)
                        curveTo(15.158f, 10.527f, 15.543f, 11.101f, 15.929f, 11.795f)
                        close()
                        moveTo(14.625f, 3.909f)
                        curveTo(14.625f, 3.906f, 14.625f, 3.902f, 14.625f, 3.898f)
                        curveTo(14.625f, 3.792f, 14.626f, 3.684f, 14.629f, 3.573f)
                        curveTo(14.637f, 3.298f, 14.655f, 3.007f, 14.685f, 2.698f)
                        curveTo(14.732f, 2.213f, 14.239f, 1.841f, 13.811f, 2.074f)
                        curveTo(13.531f, 2.227f, 13.271f, 2.381f, 13.029f, 2.538f)
                        curveTo(12.913f, 2.613f, 12.802f, 2.689f, 12.694f, 2.765f)
                        curveTo(12.686f, 2.77f, 12.678f, 2.776f, 12.67f, 2.782f)
                        curveTo(9.649f, 4.936f, 10.069f, 7.419f, 10.867f, 10.572f)
                        curveTo(11.071f, 11.377f, 10.983f, 11.902f, 10.395f, 12.229f)
                        curveTo(9.807f, 12.557f, 9.065f, 12.346f, 8.739f, 11.757f)
                        curveTo(8.64f, 11.58f, 8.554f, 11.398f, 8.481f, 11.214f)
                        curveTo(8.473f, 11.196f, 8.466f, 11.178f, 8.46f, 11.16f)
                        curveTo(8.397f, 10.997f, 8.345f, 10.833f, 8.302f, 10.667f)
                        curveTo(8.27f, 10.542f, 8.243f, 10.417f, 8.222f, 10.292f)
                        curveTo(8.12f, 9.695f, 7.333f, 9.303f, 7f, 9.809f)
                        curveTo(6.84f, 10.052f, 6.689f, 10.308f, 6.548f, 10.577f)
                        curveTo(6.482f, 10.702f, 6.418f, 10.829f, 6.357f, 10.96f)
                        curveTo(6.348f, 10.979f, 6.338f, 11f, 6.329f, 11.02f)
                        curveTo(6.115f, 11.484f, 5.93f, 11.98f, 5.781f, 12.502f)
                        curveTo(4.798f, 15.953f, 6.799f, 19.551f, 10.251f, 20.534f)
                        curveTo(13.702f, 21.517f, 17.3f, 19.515f, 18.284f, 16.064f)
                        curveTo(18.806f, 14.231f, 18.537f, 12.363f, 17.676f, 10.822f)
                        curveTo(17.242f, 10.042f, 16.812f, 9.402f, 16.417f, 8.815f)
                        curveTo(15.409f, 7.315f, 14.634f, 6.163f, 14.625f, 3.909f)
                        close()
                    }
                }.build()

        return _Trending!!
    }

@Suppress("ObjectPropertyName")
private var _Trending: ImageVector? = null
