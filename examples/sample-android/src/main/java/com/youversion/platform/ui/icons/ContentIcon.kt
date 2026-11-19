package com.youversion.platform.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val ContentIcon: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector
        .Builder(
            name = "ContentIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(12.981f, 12.3212f)
                curveTo(12.7041f, 12.4689f, 12.3333f, 12.3101f, 12.3333f, 12.0439f)
                verticalLineTo(7.9562f)
                curveTo(12.3333f, 7.69f, 12.7041f, 7.5312f, 12.981f, 7.6789f)
                lineTo(16.8132f, 9.7227f)
                curveTo(17.0606f, 9.8546f, 17.0606f, 10.1454f, 16.8132f, 10.2774f)
                lineTo(12.981f, 12.3212f)
                close()
            }
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(6.8787f, 3.8787f)
                curveTo(6f, 4.7574f, 6f, 6.1716f, 6f, 9f)
                verticalLineTo(11f)
                curveTo(6f, 13.8284f, 6f, 15.2426f, 6.8787f, 16.1213f)
                curveTo(7.7574f, 17f, 9.1716f, 17f, 12f, 17f)
                horizontalLineTo(16f)
                curveTo(18.8284f, 17f, 20.2426f, 17f, 21.1213f, 16.1213f)
                curveTo(22f, 15.2426f, 22f, 13.8284f, 22f, 11f)
                verticalLineTo(9f)
                curveTo(22f, 6.1716f, 22f, 4.7574f, 21.1213f, 3.8787f)
                curveTo(20.2426f, 3f, 18.8284f, 3f, 16f, 3f)
                horizontalLineTo(12f)
                curveTo(9.1716f, 3f, 7.7574f, 3f, 6.8787f, 3.8787f)
                close()
                moveTo(19.0253f, 5.092f)
                curveTo(18.3726f, 5.0042f, 17.4708f, 5f, 16f, 5f)
                horizontalLineTo(12f)
                curveTo(10.5292f, 5f, 9.6274f, 5.0042f, 8.9747f, 5.092f)
                curveTo(8.401f, 5.1691f, 8.3052f, 5.2788f, 8.2938f, 5.2919f)
                lineTo(8.2929f, 5.2929f)
                lineTo(8.2919f, 5.2938f)
                curveTo(8.2788f, 5.3052f, 8.1691f, 5.401f, 8.092f, 5.9747f)
                curveTo(8.0043f, 6.6274f, 8f, 7.5293f, 8f, 9f)
                verticalLineTo(11f)
                curveTo(8f, 12.4708f, 8.0043f, 13.3726f, 8.092f, 14.0253f)
                curveTo(8.1691f, 14.599f, 8.2788f, 14.6948f, 8.2919f, 14.7062f)
                lineTo(8.2929f, 14.7071f)
                lineTo(8.2938f, 14.7082f)
                curveTo(8.3052f, 14.7212f, 8.401f, 14.8309f, 8.9747f, 14.908f)
                curveTo(9.6274f, 14.9958f, 10.5292f, 15f, 12f, 15f)
                horizontalLineTo(16f)
                curveTo(17.4708f, 15f, 18.3726f, 14.9958f, 19.0253f, 14.908f)
                curveTo(19.599f, 14.8309f, 19.6948f, 14.7212f, 19.7062f, 14.7082f)
                lineTo(19.7071f, 14.7071f)
                lineTo(19.7082f, 14.7062f)
                curveTo(19.7212f, 14.6948f, 19.8309f, 14.599f, 19.908f, 14.0253f)
                curveTo(19.9958f, 13.3726f, 20f, 12.4708f, 20f, 11f)
                verticalLineTo(9f)
                curveTo(20f, 7.5293f, 19.9958f, 6.6274f, 19.908f, 5.9747f)
                curveTo(19.8309f, 5.401f, 19.7212f, 5.3052f, 19.7082f, 5.2938f)
                lineTo(19.7071f, 5.2929f)
                lineTo(19.7062f, 5.2919f)
                curveTo(19.6948f, 5.2788f, 19.599f, 5.1691f, 19.0253f, 5.092f)
                close()
            }
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(3.5005f, 7.08f)
                curveTo(3.7679f, 6.9953f, 4f, 7.2274f, 4f, 7.5078f)
                verticalLineTo(15f)
                curveTo(4f, 17.2091f, 5.7909f, 19f, 8f, 19f)
                horizontalLineTo(17.4944f)
                curveTo(17.7748f, 19f, 18.0053f, 19.2315f, 17.9275f, 19.5009f)
                curveTo(17.6945f, 20.3087f, 16.9252f, 21f, 16f, 21f)
                horizontalLineTo(8f)
                curveTo(4.6863f, 21f, 2f, 18.3137f, 2f, 15f)
                verticalLineTo(9f)
                curveTo(2f, 8.1681f, 2.6921f, 7.3361f, 3.5005f, 7.08f)
                close()
            }
        }.build()
}
