package com.youversion.platform.core.users.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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

    private fun userInfo(
        firstName: String? = "first",
        lastName: String? = "last",
        userId: String? = "user",
        avatarUrlFormat: String? = "avatar",
    ) = YouVersionUserInfo(
        firstName = firstName,
        lastName = lastName,
        userId = userId,
        avatarUrlFormat = avatarUrlFormat,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(userInfo(), userInfo())
        assertEquals(userInfo().hashCode(), userInfo().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            userInfo(firstName = null),
            userInfo(lastName = null),
            userInfo(userId = null),
            userInfo(avatarUrlFormat = null),
        ).forEach {
            assertNotEquals(userInfo(), it)
            assertNotEquals(userInfo().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals(
            "YouVersionUserInfo(firstName=first, lastName=last, userId=user, avatarUrlFormat=avatar)",
            userInfo().toString(),
        )
    }
}
