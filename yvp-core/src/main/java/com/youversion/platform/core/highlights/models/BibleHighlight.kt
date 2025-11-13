package com.youversion.platform.core.highlights.models

import com.youversion.platform.core.bibles.domain.BibleReference

data class BibleHighlight(
    val bibleReference: BibleReference,
    val hexColor: String,
) {
    override fun toString(): String = "$bibleReference : $hexColor"
}
