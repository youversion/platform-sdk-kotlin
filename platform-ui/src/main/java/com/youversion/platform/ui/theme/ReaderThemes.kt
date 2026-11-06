package com.youversion.platform.ui.theme

import androidx.compose.ui.graphics.Color
import com.youversion.platform.core.di.PlatformInternalApi

@PlatformInternalApi
public data class ReaderTheme(
    val id: Int,
    val colorScheme: ReaderColorScheme,
) {
    public companion object {
        public val allThemes: List<ReaderTheme> =
            listOf(
                ReaderTheme(1, PureWhite),
                ReaderTheme(2, Sepia),
                ReaderTheme(3, PaperGray),
                ReaderTheme(4, Cream),
                ReaderTheme(5, Charcoal),
                ReaderTheme(6, MidnightBlue),
                ReaderTheme(7, TrueBlack),
            )

        public fun themeById(id: Int? = null): ReaderTheme = allThemes.find { it.id == id } ?: allThemes.first()
    }
}

// Light themes
@PlatformInternalApi
public val PureWhite: ReaderColorScheme =
    lightReaderColorScheme(
        background = Color(0xFFFFFFFF),
        foreground = Color(0xFF121212),
    )

@PlatformInternalApi
public val Sepia: ReaderColorScheme =
    lightReaderColorScheme(
        background = Color(0xFFF6EFEE),
        foreground = Color(0xFF121212),
    )

@PlatformInternalApi
public val PaperGray: ReaderColorScheme =
    lightReaderColorScheme(
        background = Color(0xFFEDEFEF),
        foreground = Color(0xFF121212),
    )

@PlatformInternalApi
public val Cream: ReaderColorScheme =
    lightReaderColorScheme(
        background = Color(0xFFFEF5EB),
        foreground = Color(0xFF121212),
    )

// Dark themes
@PlatformInternalApi
public val Charcoal: ReaderColorScheme =
    darkReaderColorScheme(
        background = Color(0xFF2B3031),
        foreground = Color.White,
    )

@PlatformInternalApi
public val MidnightBlue: ReaderColorScheme =
    darkReaderColorScheme(
        background = Color(0xFF1C2A3B),
        foreground = Color.White,
    )

@PlatformInternalApi
public val TrueBlack: ReaderColorScheme =
    darkReaderColorScheme(
        background = Color(0xFF121212),
        foreground = Color.White,
        highlightMixRatio = 0.25f,
    )
