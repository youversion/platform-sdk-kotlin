package com.youversion.platform.reader.sheets

import androidx.compose.ui.graphics.Color

/**
 * The highlight colors offered by the verse action sheet color picker.
 *
 * Each entry pairs the Compose [color] used to render the swatch with the
 * [hexColor] string consumed by the highlight write actions and color-presence
 * helpers on `BibleReaderViewModel`. Values match the Swift SDK palette.
 */
internal enum class HighlightColor(
    val color: Color,
    val hexColor: String,
) {
    Yellow(Color(0xFFFFFE00), "fffe00"),
    Green(Color(0xFF5DFF79), "5dff79"),
    Cyan(Color(0xFF00D6FF), "00d6ff"),
    Orange(Color(0xFFFFC66F), "ffc66f"),
    Pink(Color(0xFFFF95EF), "ff95ef"),
}
