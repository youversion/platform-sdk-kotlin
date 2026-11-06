package com.youversion.platform.ui.theme.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.ui.theme.ReaderColorScheme

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

@PlatformInternalApi
public object BibleReaderTheme {
    // Observable state for the selected color scheme. Might deprecate.
    public val selectedColorScheme: MutableState<ReaderColorScheme?> = mutableStateOf<ReaderColorScheme?>(null)

    public val colorScheme: BibleReaderColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalBibleReaderColorScheme.current

    public val typography: BibleReaderTypography
        @Composable @ReadOnlyComposable
        get() = LocalBibleReaderTypography.current
}
