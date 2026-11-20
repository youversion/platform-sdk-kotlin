package com.youversion.platform.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val BibleCardIcon: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector
        .Builder(
            name = "BibleCardIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
            ) {
                moveTo(8f, 2.8f)
                horizontalLineTo(16f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18.5f, 5.3f)
                verticalLineTo(18.7f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 21.2f)
                horizontalLineTo(8f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5.5f, 18.7f)
                verticalLineTo(5.3f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8f, 2.8f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(8.8f, 7.5f)
                horizontalLineTo(15.2f)
                moveTo(8.8f, 10.5f)
                horizontalLineTo(15.2f)
                moveTo(8.8f, 13.5f)
                horizontalLineTo(12.5f)
            }
        }.build()
}
