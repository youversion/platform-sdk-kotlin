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
 * accessibility services. Values match the Swift SDK palette.
 */
internal enum class HighlightColor(
    val color: Color,
    val hexColor: String,
    @StringRes val nameResId: Int,
) {
    Yellow(Color(0xFFFFFE00), "fffe00", R.string.highlight_color_yellow),
    Green(Color(0xFF5DFF79), "5dff79", R.string.highlight_color_green),
    Cyan(Color(0xFF00D6FF), "00d6ff", R.string.highlight_color_cyan),
    Orange(Color(0xFFFFC66F), "ffc66f", R.string.highlight_color_orange),
    Pink(Color(0xFFFF95EF), "ff95ef", R.string.highlight_color_pink),
}
