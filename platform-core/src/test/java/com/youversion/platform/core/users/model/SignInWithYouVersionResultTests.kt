package com.youversion.platform.core.users.model

import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SignInWithYouVersionResultTests {
    private fun result(
        accessToken: String? = "access",
        expiryDate: Date? = Date(1000L),
        refreshToken: String? = "refresh",
        idToken: String? = "idToken",
        permissions: List<SignInWithYouVersionPermission> = listOf(SignInWithYouVersionPermission.EMAIL),
        yvpUserId: String? = "user",
        name: String? = "name",
        profilePicture: String? = "picture",
        email: String? = "email",
    ) = SignInWithYouVersionResult(
        accessToken = accessToken,
        expiryDate = expiryDate,
        refreshToken = refreshToken,
        idToken = idToken,
        permissions = permissions,
        yvpUserId = yvpUserId,
        name = name,
        profilePicture = profilePicture,
        email = email,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(result(), result())
        assertEquals(result().hashCode(), result().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            result(accessToken = null),
            result(expiryDate = null),
            result(refreshToken = null),
            result(idToken = null),
            result(permissions = emptyList()),
            result(yvpUserId = null),
            result(name = null),
            result(profilePicture = null),
            result(email = null),
        ).forEach {
            assertNotEquals(result(), it)
            assertNotEquals(result().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString masks tokens and email`() {
        assertEquals(
            "SignInWithYouVersionResult(accessToken=<redacted>, expiryDate=${Date(1000L)}, " +
                "refreshToken=<redacted>, idToken=<redacted>, " +
                "permissions=[${SignInWithYouVersionPermission.EMAIL}], yvpUserId=user, name=name, " +
                "profilePicture=picture, email=<redacted>)",
            result().toString(),
        )
    }

    @Test
    fun `test toString shows absent tokens and email as null`() {
        val text = result(accessToken = null, refreshToken = null, idToken = null, email = null).toString()
        assertEquals(true, text.contains("accessToken=null"))
        assertEquals(true, text.contains("refreshToken=null"))
        assertEquals(true, text.contains("idToken=null"))
        assertEquals(true, text.contains("email=null"))
    }
}
