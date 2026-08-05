package com.youversion.platform.ui.theme

import androidx.compose.ui.graphics.Color

data class ReaderColorScheme(
    val isDark: Boolean,
    val background: Color,
    val foreground: Color,
    val surfacePrimaryColor: Color,
    val surfaceTertiaryColor: Color,
    val borderPrimaryColor: Color,
    val borderSecondaryColor: Color,
    val buttonPrimaryColor: Color,
    val buttonSecondaryColor: Color,
    val buttonContrastColor: Color,
    val textInvertedColor: Color,
    val readerWhiteColor: Color,
    val readerBlackColor: Color,
    val dropShadowColor: Color,
    val wordsOfChristColor: Color,
) {
    val readerCanvasPrimaryColor: Color
        get() = background
    val readerTextPrimaryColor: Color
        get() = foreground
    val readerTextMutedColor: Color
        get() = borderSecondaryColor

    /**
     * The opacity that highlight colors are drawn at under this scheme.
     *
     * The highlight palette is chosen to sit behind dark text on a light page, so at full strength it
     * overwhelms the light text of a dark scheme. Dimming keeps the highlight behind the words while
     * still showing which verses are highlighted and in which color.
     */
    val highlightAlpha: Float
        get() = if (isDark) 0.3f else 1f
}

fun lightReaderColorScheme(
    background: Color,
    foreground: Color,
    surfacePrimaryColor: Color = LightSurfacePrimaryColor,
    surfaceTertiaryColor: Color = LightSurfaceTertiaryColor,
    borderPrimaryColor: Color = LightBorderPrimaryColor,
    borderSecondaryColor: Color = LightBorderSecondaryColor,
    buttonPrimaryColor: Color = LightButtonPrimaryColor,
    buttonSecondaryColor: Color = LightButtonSecondaryColor,
    buttonContrastColor: Color = LightButtonContrastColor,
    textInvertedColor: Color = LightTextInvertedColor,
    readerWhiteColor: Color = ReaderWhiteColor,
    readerBlackColor: Color = ReaderBlackColor,
    dropShadowColor: Color = ReaderDropShadowColor,
    wordsOfChristColor: Color = LightWordsOfChristColor,
) = ReaderColorScheme(
    isDark = false,
    background = background,
    foreground = foreground,
    surfacePrimaryColor = surfacePrimaryColor,
    surfaceTertiaryColor = surfaceTertiaryColor,
    borderPrimaryColor = borderPrimaryColor,
    borderSecondaryColor = borderSecondaryColor,
    buttonPrimaryColor = buttonPrimaryColor,
    buttonSecondaryColor = buttonSecondaryColor,
    buttonContrastColor = buttonContrastColor,
    textInvertedColor = textInvertedColor,
    readerWhiteColor = readerWhiteColor,
    readerBlackColor = readerBlackColor,
    dropShadowColor = dropShadowColor,
    wordsOfChristColor = wordsOfChristColor,
)

fun darkReaderColorScheme(
    background: Color,
    foreground: Color,
    surfacePrimaryColor: Color = DarkSurfacePrimaryColor,
    surfaceTertiaryColor: Color = DarkSurfaceTertiaryColor,
    borderPrimaryColor: Color = DarkBorderPrimaryColor,
    borderSecondaryColor: Color = DarkBorderSecondaryColor,
    buttonPrimaryColor: Color = DarkButtonPrimaryColor,
    buttonSecondaryColor: Color = DarkButtonSecondaryColor,
    buttonContrastColor: Color = DarkButtonContrastColor,
    textInvertedColor: Color = DarkTextInvertedColor,
    readerWhiteColor: Color = ReaderWhiteColor,
    readerBlackColor: Color = ReaderBlackColor,
    dropShadowColor: Color = ReaderDropShadowColor,
    wordsOfChristColor: Color = DarkWordsOfChristColor,
) = ReaderColorScheme(
    isDark = true,
    background = background,
    foreground = foreground,
    surfacePrimaryColor = surfacePrimaryColor,
    surfaceTertiaryColor = surfaceTertiaryColor,
    borderPrimaryColor = borderPrimaryColor,
    borderSecondaryColor = borderSecondaryColor,
    buttonPrimaryColor = buttonPrimaryColor,
    buttonSecondaryColor = buttonSecondaryColor,
    buttonContrastColor = buttonContrastColor,
    textInvertedColor = textInvertedColor,
    readerWhiteColor = readerWhiteColor,
    readerBlackColor = readerBlackColor,
    dropShadowColor = dropShadowColor,
    wordsOfChristColor = wordsOfChristColor,
)
