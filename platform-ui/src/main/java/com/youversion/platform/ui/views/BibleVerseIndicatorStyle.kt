package com.youversion.platform.ui.views

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * How a marked verse number is drawn and tapped.
 *
 * The marker is a rounded fill behind the verse number, drawn over any highlight on that verse. Its tap area is
 * centered on the number and grows to at least [minimumTouchTargetSize] without changing how the text is laid out.
 *
 * @property backgroundColor The fill behind the verse number.
 * @property foregroundColor The verse number's color inside the marker, or null to keep its usual color.
 * @property minimumTouchTargetSize The smallest area a tap on the marker is accepted in. The default is the
 * 48dp accessibility minimum.
 * @property contentDescription What accessibility services read for the marker, or null to read the verse number.
 */
public class BibleVerseIndicatorStyle(
    public val backgroundColor: Color,
    public val foregroundColor: Color? = null,
    public val minimumTouchTargetSize: DpSize = DpSize(48.dp, 48.dp),
    public val contentDescription: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BibleVerseIndicatorStyle) return false
        return backgroundColor == other.backgroundColor &&
            foregroundColor == other.foregroundColor &&
            minimumTouchTargetSize == other.minimumTouchTargetSize &&
            contentDescription == other.contentDescription
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + foregroundColor.hashCode()
        result = 31 * result + minimumTouchTargetSize.hashCode()
        result = 31 * result + contentDescription.hashCode()
        return result
    }

    override fun toString(): String =
        "BibleVerseIndicatorStyle(backgroundColor=$backgroundColor, foregroundColor=$foregroundColor, " +
            "minimumTouchTargetSize=$minimumTouchTargetSize, contentDescription=$contentDescription)"
}
