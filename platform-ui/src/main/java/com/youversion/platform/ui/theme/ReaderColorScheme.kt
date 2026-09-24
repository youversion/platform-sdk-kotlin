package com.youversion.platform.ui.theme

import androidx.compose.ui.graphics.Color
import com.youversion.platform.core.di.PlatformInternalApi

@PlatformInternalApi
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
    val highlightMixRatio: Float = 1f,
) {
    val readerCanvasPrimaryColor: Color
        get() = background
    val readerTextPrimaryColor: Color
        get() = foreground
    val readerTextMutedColor: Color
        get() = if (isDark) DarkTextMutedColor else LightTextMutedColor

    /**
     * [highlightColor] mixed into this scheme's [background] by [highlightMixRatio].
     *
     * The highlight palette is chosen to sit behind dark text on a light page, so at full strength it
     * overwhelms the light text of a dark scheme. Mixing keeps the highlight behind the words while
     * still showing which verses are highlighted and in which color.
     *
     * Any alpha carried by [highlightColor] is dropped; the returned color is opaque.
     */
    fun mixedHighlightColor(highlightColor: Color): Color = mixSrgb(highlightColor, background, highlightMixRatio)
}

/**
 * [colorA] and [colorB] mixed in sRGB, with [ratio] the share taken from [colorA].
 *
 * Each channel is blended independently, so the result tracks the source colors without the
 * gamma shift a perceptual blend would introduce. Only the RGB channels take part and the result is
 * opaque, so what the mix computes is what gets painted rather than being composited a second time
 * over whatever sits behind it.
 */
internal fun mixSrgb(
    colorA: Color,
    colorB: Color,
    ratio: Float,
): Color {
    val clampedRatio = ratio.coerceIn(0f, 1f)

    fun mix(
        channelA: Float,
        channelB: Float,
    ) = channelA * clampedRatio + channelB * (1f - clampedRatio)

    return Color(
        red = mix(colorA.red, colorB.red),
        green = mix(colorA.green, colorB.green),
        blue = mix(colorA.blue, colorB.blue),
    )
}

internal fun lightReaderColorScheme(
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
    highlightMixRatio: Float = 1f,
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
    highlightMixRatio = highlightMixRatio,
)

internal fun darkReaderColorScheme(
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
    highlightMixRatio: Float = 0.2f,
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
    highlightMixRatio = highlightMixRatio,
)
