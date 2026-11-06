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
