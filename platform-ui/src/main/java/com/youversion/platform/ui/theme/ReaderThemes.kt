package com.youversion.platform.ui.theme

import androidx.compose.ui.graphics.Color
import com.youversion.platform.core.di.PlatformInternalApi

@PlatformInternalApi
data class ReaderTheme(
    val id: Int,
    val colorScheme: ReaderColorScheme,
) {
    companion object {
        val allThemes =
            listOf(
                ReaderTheme(1, PureWhite),
                ReaderTheme(2, Sepia),
                ReaderTheme(3, PaperGray),
                ReaderTheme(4, Cream),
                ReaderTheme(5, Charcoal),
                ReaderTheme(6, MidnightBlue),
                ReaderTheme(7, TrueBlack),
            )

        fun themeById(id: Int? = null): ReaderTheme = allThemes.find { it.id == id } ?: allThemes.first()
    }
}

// Light themes
@PlatformInternalApi
val PureWhite =
    lightReaderColorScheme(
        background = Color(0xFFFFFFFF),
        foreground = Color(0xFF121212),
    )

@PlatformInternalApi
val Sepia =
    lightReaderColorScheme(
        background = Color(0xFFF6EFEE),
        foreground = Color(0xFF121212),
    )

@PlatformInternalApi
val PaperGray =
    lightReaderColorScheme(
        background = Color(0xFFEDEFEF),
        foreground = Color(0xFF121212),
    )

@PlatformInternalApi
val Cream =
    lightReaderColorScheme(
        background = Color(0xFFFEF5EB),
        foreground = Color(0xFF121212),
    )

// Dark themes
@PlatformInternalApi
val Charcoal =
    darkReaderColorScheme(
        background = Color(0xFF2B3031),
        foreground = Color.White,
    )

@PlatformInternalApi
val MidnightBlue =
    darkReaderColorScheme(
        background = Color(0xFF1C2A3B),
        foreground = Color.White,
    )

@PlatformInternalApi
val TrueBlack =
    darkReaderColorScheme(
        background = Color(0xFF121212),
        foreground = Color.White,
    )
