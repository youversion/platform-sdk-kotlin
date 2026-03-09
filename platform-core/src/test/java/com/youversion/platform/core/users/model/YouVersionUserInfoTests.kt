package com.youversion.platform.core.users.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YouVersionUserInfoTests {
    @Test
    fun `test avatarUrl returns null when avatarUrlFormat is null`() {
        val userInfo = YouVersionUserInfo(avatarUrlFormat = null)
        assertNull(userInfo.avatarUrl)
    }

    @Test
    fun `test avatarUrl returns null when avatarUrlFormat is empty`() {
        val userInfo = YouVersionUserInfo(avatarUrlFormat = "")
        assertNull(userInfo.avatarUrl)
    }

    @Test
    fun `test avatarUrl returns null when avatarUrlFormat is blank`() {
        val userInfo = YouVersionUserInfo(avatarUrlFormat = "   ")
        assertNull(userInfo.avatarUrl)
    }

    @Test
    fun `test avatarUrl prepends https when URL is protocol-relative`() {
        val userInfo = YouVersionUserInfo(avatarUrlFormat = "//example.com/avatar.jpg")
        assertEquals("https://example.com/avatar.jpg", userInfo.avatarUrl)
    }

    @Test
    fun `test avatarUrl replaces width and height placeholders with 200`() {
        val userInfo = YouVersionUserInfo(avatarUrlFormat = "https://example.com/avatar_{width}x{height}.jpg")
        assertEquals("https://example.com/avatar_200x200.jpg", userInfo.avatarUrl)
    }

    @Test
    fun `test avatarUrl prepends https and replaces placeholders for protocol-relative URL with placeholders`() {
        val userInfo = YouVersionUserInfo(avatarUrlFormat = "//example.com/avatar_{width}x{height}.jpg")
        assertEquals("https://example.com/avatar_200x200.jpg", userInfo.avatarUrl)
    }
}
