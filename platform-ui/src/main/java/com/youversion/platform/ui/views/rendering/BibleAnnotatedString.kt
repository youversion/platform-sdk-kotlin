package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.AnnotatedString
import com.youversion.platform.core.di.PlatformInternalApi

internal fun AnnotatedString.Builder.addTextCategoryAnnotation(
    category: BibleTextCategory,
    start: Int,
    end: Int,
) {
    addStringAnnotation(
        tag = BibleTextCategoryAttribute.NAME,
        annotation = category.name,
        start = start,
        end = end,
    )
}

internal fun AnnotatedString.trimTrailingWhitespace(): AnnotatedString {
    var endIndex = this.text.length - 1
    while (endIndex >= 0 && this.text[endIndex].isWhitespace()) {
        endIndex--
    }

    if (endIndex < this.text.length - 1) {
        return this.subSequence(0, endIndex + 1)
    }

    return this
}

@PlatformInternalApi
public object BibleReferenceAttribute {
    public const val NAME: String = "BibleReference"
}

@PlatformInternalApi
public object BibleTextCategoryAttribute {
    public const val NAME: String = "BibleTextCategory"
}
