package com.youversion.platform.core.highlights.models

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.di.PlatformInternalApi

public class BibleHighlight
    @PlatformInternalApi
    constructor(
        public val bibleReference: BibleReference,
        public val hexColor: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BibleHighlight) return false
            return bibleReference == other.bibleReference &&
                hexColor == other.hexColor
        }

        override fun hashCode(): Int {
            var result = bibleReference.hashCode()
            result = 31 * result + hexColor.hashCode()
            return result
        }

        override fun toString(): String = "$bibleReference : $hexColor"
    }

/**
 * Whether two highlight hex colors name the same red, green and blue, as the Swift SDK compares them. Each may have a
 * leading `#` and be three digits (`abc` for `aabbcc`), six, or eight with a leading alpha that is ignored, so a note's
 * `80dfdcff` matches the palette's `dfdcff`. Colors that are not one of these match only when their text does, ignoring
 * case and the `#`.
 */
@PlatformInternalApi
public fun isSameHighlightColor(
    first: String,
    second: String,
): Boolean {
    val firstRgb = highlightRgb(first)
    val secondRgb = highlightRgb(second)
    if (firstRgb == null || secondRgb == null) {
        return first.removePrefix("#").equals(second.removePrefix("#"), ignoreCase = true)
    }
    return firstRgb == secondRgb
}

private fun highlightRgb(hexColor: String): Long? {
    val digits = hexColor.removePrefix("#")
    if (!digits.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    val value = digits.toLongOrNull(radix = 16) ?: return null
    return when (digits.length) {
        3 -> {
            val red = (value shr 8) and 0xF
            val green = (value shr 4) and 0xF
            val blue = value and 0xF
            (red * 0x11 shl 16) or (green * 0x11 shl 8) or (blue * 0x11)
        }

        6 -> {
            value
        }

        8 -> {
            value and 0xFFFFFF
        }

        else -> {
            null
        }
    }
}
