package com.youversion.platform.reader.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import com.youversion.platform.reader.theme.ui.darkBibleReaderColorScheme
import com.youversion.platform.reader.theme.ui.lightBibleReaderColorScheme

private val LocalReaderColorScheme = staticCompositionLocalOf { PureWhite }

val MaterialTheme.readerColorScheme: ReaderColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalReaderColorScheme.current

@Composable
fun BibleReaderMaterialTheme(
    readerColorScheme: ReaderColorScheme? = null,
    content: @Composable () -> Unit,
) {
    val readerColorScheme =
        readerColorScheme
            ?: BibleReaderTheme.selectedColorScheme.value
            ?: if (isSystemInDarkTheme()) MidnightBlue else Cream

    val bibleReaderColorScheme =
        if (readerColorScheme.isDark) {
            darkBibleReaderColorScheme()
        } else {
            lightBibleReaderColorScheme()
        }
    val colorScheme =
        if (readerColorScheme.isDark) {
            darkColorScheme(
                background = readerColorScheme.background,
                onBackground = readerColorScheme.foreground,
                surface = readerColorScheme.surfacePrimaryColor,
                surfaceContainer = readerColorScheme.surfacePrimaryColor,
                surfaceVariant = readerColorScheme.surfaceTertiaryColor,
                onSurface = readerColorScheme.foreground,
                onSurfaceVariant = readerColorScheme.foreground,
                outline = readerColorScheme.borderPrimaryColor,
                outlineVariant = readerColorScheme.borderSecondaryColor,
            )
        } else {
            lightColorScheme(
                background = readerColorScheme.background,
                onBackground = readerColorScheme.foreground,
                surface = readerColorScheme.surfacePrimaryColor,
                surfaceContainer = readerColorScheme.surfacePrimaryColor,
                surfaceVariant = readerColorScheme.surfaceTertiaryColor,
                onSurface = readerColorScheme.foreground,
                onSurfaceVariant = readerColorScheme.foreground,
                outline = readerColorScheme.borderPrimaryColor,
                outlineVariant = readerColorScheme.borderSecondaryColor,
            )
        }

    BibleReaderTheme(
        colorScheme = bibleReaderColorScheme,
    ) {
        CompositionLocalProvider(LocalReaderColorScheme provides readerColorScheme) {
            MaterialTheme(
                colorScheme = colorScheme,
                content = content,
            )
        }
    }
}
