package com.youversion.platform.reader.domain

import android.content.ClipboardManager
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class CopyManagerTest {
    @Test
    fun `copyText sets primary clip with expected label and text`() {
        val application = RuntimeEnvironment.getApplication()
        val manager = CopyManager(application)

        manager.copyText(label = "Label", text = "Body")

        val clipboardManager = application.getSystemService(ClipboardManager::class.java)
        val clipData = clipboardManager.primaryClip

        assertNotNull(clipData)
        assertEquals("Label", clipData.description.label?.toString())
        assertEquals("Body", clipData.getItemAt(0).coerceToText(application).toString())
    }
}
