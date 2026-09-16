package com.youversion.platform.core.utilities

import com.youversion.platform.core.di.PlatformInternalApi
import java.text.BreakIterator
import java.util.Locale

/**
 * Counts what a reader perceives as characters, not UTF-16 code units; `length` is not equivalent.
 * Pinned to [Locale.ROOT] so a query is accepted or rejected identically on every device.
 */
@PlatformInternalApi
val String.graphemeClusterCount: Int
    get() {
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(this)
        var count = 0
        while (iterator.next() != BreakIterator.DONE) count++
        return count
    }
