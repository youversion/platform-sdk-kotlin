package com.youversion.platform.ui.icons

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IconMirroringTest {
    @Test
    fun `the forward chevron mirrors in RTL`() {
        assertTrue(ChevronForwardIcon.autoMirror)
    }

    @Test
    fun `the brand logos do not mirror in RTL`() {
        listOf(YvBibleAppIcon, YvBibleAppDarkIcon, YvBibleAppLightIcon, YvPlatformDarkIcon, YvPlatformLightIcon)
            .forEach { assertFalse(it.autoMirror, "${it.name} mirrors") }
    }
}
