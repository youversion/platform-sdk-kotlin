package com.youversion.platform.reader.theme

import androidx.compose.ui.graphics.Color

// Light themes
internal val PureWhite =
    lightReaderColorScheme(
        background = Color(0xFFFFFFFF),
        foreground = Color(0xFF121212),
    )

internal val Sepia =
    lightReaderColorScheme(
        background = Color(0xFFF6EFEE),
        foreground = Color(0xFF121212),
    )

internal val PaperGray =
    lightReaderColorScheme(
        background = Color(0xFFEDEFEF),
        foreground = Color(0xFF121212),
    )

internal val Cream =
    lightReaderColorScheme(
        background = Color(0xFFFEF5EB),
        foreground = Color(0xFF121212),
    )

// Dark themes
internal val Charcoal =
    darkReaderColorScheme(
        background = Color(0xFF2B3031),
        foreground = Color.White,
    )

internal val MidnightBlue =
    darkReaderColorScheme(
        background = Color(0xFF1C2A3B),
        foreground = Color.White,
    )

internal val TrueBlack =
    darkReaderColorScheme(
        background = Color(0xFF121212),
        foreground = Color.White,
    )
