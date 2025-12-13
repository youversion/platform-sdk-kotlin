package com.youversion.platform.reader.theme.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import com.youversion.platform.reader.theme.ReaderColorScheme

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
    // Observable state for the selected color scheme. Might deprecate.
    val selectedColorScheme = mutableStateOf<ReaderColorScheme?>(null)

    val colorScheme: BibleReaderColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalBibleReaderColorScheme.current

    val typography: BibleReaderTypography
        @Composable @ReadOnlyComposable
        get() = LocalBibleReaderTypography.current
}
