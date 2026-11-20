package com.youversion.platform.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val BibleFilledIcon: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector
        .Builder(
            name = "BibleFilledIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(12f, 2f)
                curveTo(13.8289f, 2f, 15.4018f, 1.9956f, 16.6553f, 2.1641f)
                curveTo(17.9663f, 2.3403f, 19.2229f, 2.7385f, 20.2422f, 3.7578f)
                curveTo(21.2615f, 4.7771f, 21.6597f, 6.0337f, 21.8359f, 7.3447f)
                curveTo(22.0044f, 8.5982f, 22f, 10.1711f, 22f, 12f)
                curveTo(22f, 13.8289f, 22.0044f, 15.4018f, 21.8359f, 16.6553f)
                curveTo(21.6597f, 17.9663f, 21.2615f, 19.2229f, 20.2422f, 20.2422f)
                curveTo(19.2229f, 21.2615f, 17.9663f, 21.6597f, 16.6553f, 21.8359f)
                curveTo(15.4018f, 22.0044f, 13.8289f, 22f, 12f, 22f)
                curveTo(10.1711f, 22f, 8.5982f, 22.0044f, 7.3447f, 21.8359f)
                curveTo(6.0337f, 21.6597f, 4.7771f, 21.2615f, 3.7578f, 20.2422f)
                curveTo(2.7385f, 19.2229f, 2.3403f, 17.9663f, 2.1641f, 16.6553f)
                curveTo(1.9956f, 15.4018f, 2f, 13.8289f, 2f, 12f)
                curveTo(2f, 10.1711f, 1.9956f, 8.5982f, 2.1641f, 7.3447f)
                curveTo(2.3403f, 6.0337f, 2.7385f, 4.7771f, 3.7578f, 3.7578f)
                curveTo(4.7771f, 2.7385f, 6.0337f, 2.3403f, 7.3447f, 2.1641f)
                curveTo(8.5982f, 1.9956f, 10.1711f, 2f, 12f, 2f)
                close()
                moveTo(19.4932f, 13.8008f)
                curveTo(19.0974f, 15.1147f, 17.9474f, 15.9999f, 15.9229f, 16f)
                horizontalLineTo(6.9307f)
                curveTo(6.1291f, 16f, 5.3082f, 16.3338f, 5.2031f, 17.2188f)
                curveTo(5.11f, 18.0049f, 5.6122f, 18.6486f, 6.0576f, 18.8828f)
                curveTo(6.8904f, 19.3207f, 8.1663f, 19.5f, 10.2432f, 19.5f)
                horizontalLineTo(13.2402f)
                curveTo(14.5425f, 19.5f, 16.1289f, 19.4727f, 17.0391f, 19.2266f)
                curveTo(17.9492f, 18.9805f, 18.707f, 18.5273f, 19.0977f, 17.4688f)
                curveTo(19.3959f, 16.6607f, 19.4932f, 15.1989f, 19.4932f, 13.8008f)
                close()
            }
        }.build()
}
