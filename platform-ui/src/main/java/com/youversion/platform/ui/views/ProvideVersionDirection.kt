package com.youversion.platform.ui.views

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Lays out [content] and its text in the Bible version's direction rather than the device's, so a right-to-left
 * version reads right to left even where a block opens with left-to-right characters.
 */
@Composable
internal fun ProvideVersionDirection(
    isRightToLeft: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalTextStyle provides
            LocalTextStyle.current.copy(textDirection = if (isRightToLeft) TextDirection.Rtl else TextDirection.Ltr),
        content = content,
    )
}
