package com.youversion.platform.reader.domain

import android.content.Context
import android.content.Intent

/**
 * Manages launching the Android share sheet for sharing plain text content.
 */
class ShareManager(
    private val context: Context,
) {
    /**
     * Launches the Android share sheet with the given text and title.
     */
    fun shareText(
        text: String,
        title: String,
    ) {
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_TITLE, title)
                type = "text/plain"
            }
        val chooserIntent =
            Intent.createChooser(sendIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(chooserIntent)
    }
}
