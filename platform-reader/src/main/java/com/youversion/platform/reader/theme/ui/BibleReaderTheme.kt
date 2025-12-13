package com.youversion.platform.reader.theme.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
internal fun BibleReaderTheme(
    colorScheme: BibleReaderColorScheme = BibleReaderTheme.colorScheme,
    typography: BibleReaderTypography = BibleReaderTheme.typography,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBibleReaderColorScheme provides colorScheme,
        LocalBibleReaderTypography provides typography,
    ) {
        content()
    }
}

object BibleReaderTheme {
    val colorScheme: BibleReaderColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalBibleReaderColorScheme.current

    val typography: BibleReaderTypography
        @Composable @ReadOnlyComposable
        get() = LocalBibleReaderTypography.current
}
