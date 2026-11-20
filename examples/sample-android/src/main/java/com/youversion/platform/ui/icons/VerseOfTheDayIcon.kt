package com.youversion.platform.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val VerseOfTheDayIcon: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector
        .Builder(
            name = "VerseOfTheDayIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(12f, 7.5f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 16.5f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 7.5f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 2.5f)
                verticalLineTo(4.5f)
                moveTo(12f, 19.5f)
                verticalLineTo(21.5f)
                moveTo(2.5f, 12f)
                horizontalLineTo(4.5f)
                moveTo(19.5f, 12f)
                horizontalLineTo(21.5f)
                moveTo(5.28f, 5.28f)
                lineTo(6.7f, 6.7f)
                moveTo(17.3f, 17.3f)
                lineTo(18.72f, 18.72f)
                moveTo(5.28f, 18.72f)
                lineTo(6.7f, 17.3f)
                moveTo(17.3f, 6.7f)
                lineTo(18.72f, 5.28f)
            }
        }.build()
}
