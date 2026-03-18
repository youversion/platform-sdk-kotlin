package com.youversion.platform.ui.signin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class PKCEStateStoreTests {
    private lateinit var context: Context

    @BeforeTest
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @AfterTest
    @AfterTest
    fun teardown() {
        PKCEStateStore.clear(context)
        PKCEStateStore::class.java.getDeclaredField("prefs").apply {
            isAccessible = true
            set(PKCEStateStore, null)
        }
    }

    // ----- save and retrieve

    @Test
    fun `test save stores codeVerifier that can be retrieved`() {
        PKCEStateStore.save(context, codeVerifier = "verifier", state = "state", nonce = "nonce")
        assertEquals("verifier", PKCEStateStore.getCodeVerifier(context))
    }

    @Test
    fun `test save stores state that can be retrieved`() {
        PKCEStateStore.save(context, codeVerifier = "verifier", state = "state", nonce = "nonce")
        assertEquals("state", PKCEStateStore.getState(context))
    }

    @Test
    fun `test save stores nonce that can be retrieved`() {
        PKCEStateStore.save(context, codeVerifier = "verifier", state = "state", nonce = "nonce")
        assertEquals("nonce", PKCEStateStore.getNonce(context))
    }

    // ----- get returns null when nothing is saved

    @Test
    fun `test getCodeVerifier returns null when nothing is saved`() {
        assertNull(PKCEStateStore.getCodeVerifier(context))
    }

    @Test
    fun `test getState returns null when nothing is saved`() {
        assertNull(PKCEStateStore.getState(context))
    }

    @Test
    fun `test getNonce returns null when nothing is saved`() {
        assertNull(PKCEStateStore.getNonce(context))
    }

    // ----- clear

    @Test
    fun `test clear removes all stored values`() {
        PKCEStateStore.save(context, codeVerifier = "verifier", state = "state", nonce = "nonce")
        PKCEStateStore.clear(context)
        assertNull(PKCEStateStore.getCodeVerifier(context))
        assertNull(PKCEStateStore.getState(context))
        assertNull(PKCEStateStore.getNonce(context))
    }

    // ----- overwrite

    @Test
    fun `test save overwrites previously stored values`() {
        PKCEStateStore.save(context, codeVerifier = "first", state = "first", nonce = "first")
        PKCEStateStore.save(context, codeVerifier = "second", state = "second", nonce = "second")
        assertEquals("second", PKCEStateStore.getCodeVerifier(context))
        assertEquals("second", PKCEStateStore.getState(context))
        assertEquals("second", PKCEStateStore.getNonce(context))
    }
}
