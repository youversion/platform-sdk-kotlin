package com.youversion.platform.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val BookClosedIcon: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector
        .Builder(
            name = "BookClosedIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(19.1f, 15.9f)
                verticalLineTo(4.6f)
                arcTo(1.8f, 1.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 17.3f, 2.8f)
                horizontalLineTo(6.2f)
                arcTo(1.8f, 1.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4.4f, 4.6f)
                verticalLineTo(18.9f)
                arcTo(1.8f, 1.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6.2f, 20.7f)
                horizontalLineTo(19.1f)
                moveTo(4.4f, 15.9f)
                horizontalLineTo(19.1f)
                moveTo(17f, 15.9f)
                verticalLineTo(20.7f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(7.3f, 2.8f)
                verticalLineTo(15.9f)
            }
        }.build()
}
