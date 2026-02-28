package com.youversion.platform.reader.domain

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Manages copying plain text content to the system clipboard.
 */
class CopyManager(
    context: Context,
) {
    private val clipboardManager = context.getSystemService(ClipboardManager::class.java)

    /**
     * Copies the given text to the system clipboard with the specified label.
     */
    fun copyText(
        label: String,
        text: String,
    ) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
