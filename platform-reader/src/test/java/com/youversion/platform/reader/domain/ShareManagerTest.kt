package com.youversion.platform.reader.domain

import android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ShareManagerTest {
    @Test
    fun `shareText starts chooser with send intent text title and plain text type`() {
        val application = RuntimeEnvironment.getApplication()
        val manager = ShareManager(application)
        val text = "some text"
        val title = "some title"

        manager.shareText(text = text, title = title)

        val chooserIntent = shadowOf(application).nextStartedActivity
        assertNotNull(chooserIntent)
        assertTrue((chooserIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)

        @Suppress("DEPRECATION")
        val sendIntent = chooserIntent.getParcelableExtra(Intent.EXTRA_INTENT) as Intent?
        assertNotNull(sendIntent)
        assertEquals(Intent.ACTION_SEND, sendIntent.action)
        assertEquals(text, sendIntent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(title, sendIntent.getStringExtra(Intent.EXTRA_TITLE))
        assertEquals("text/plain", sendIntent.type)
    }
}
