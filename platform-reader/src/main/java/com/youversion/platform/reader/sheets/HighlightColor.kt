package com.youversion.platform.reader.sheets

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.youversion.platform.reader.R

/**
 * The highlight colors offered by the verse action sheet color picker.
 *
 * Each entry pairs the Compose [color] used to render the swatch with the
 * [hexColor] string consumed by the highlight write actions and color-presence
 * helpers on `BibleReaderViewModel`, plus the localized [nameResId] announced by
 * accessibility services.
 */
internal enum class HighlightColor(
    val color: Color,
    val hexColor: String,
    @StringRes val nameResId: Int,
) {
    Yellow(Color(0xFFFFEC5B), "ffec5b", R.string.highlight_color_yellow),
    Green(Color(0xFFB4FFC1), "b4ffc1", R.string.highlight_color_green),
    Blue(Color(0xFFBBF4FF), "bbf4ff", R.string.highlight_color_blue),
    Orange(Color(0xFFFFDCA7), "ffdca7", R.string.highlight_color_orange),
    Pink(Color(0xFFFFCFF8), "ffcff8", R.string.highlight_color_pink),
    Purple(Color(0xFFDFDCFF), "dfdcff", R.string.highlight_color_purple),
}
