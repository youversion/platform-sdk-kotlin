package com.youversion.platform.core

import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ConfigTests {
    private fun config(
        appKey: String? = "appKey",
        authCallback: String = "authCallback",
        apiHost: String = "apiHost",
        hostEnv: String? = "hostEnv",
        installId: String? = "installId",
        accessToken: String? = "accessToken",
        refreshToken: String? = "refreshToken",
        idToken: String? = "idToken",
        expiryDate: Date? = Date(1000L),
        permittedLanguageTags: Set<String>? = setOf("en"),
        permittedVersionIds: Set<Int>? = setOf(1),
        excludedVersionIds: Set<Int> = setOf(2),
        grantedPermissions: Set<SignInWithYouVersionPermission> = setOf(SignInWithYouVersionPermission.EMAIL),
        isSignInEnabled: Boolean = true,
        appName: String? = "appName",
        signInPromptMessage: String? = "message",
    ) = Config(
        appKey = appKey,
        authCallback = authCallback,
        apiHost = apiHost,
        hostEnv = hostEnv,
        installId = installId,
        accessToken = accessToken,
        refreshToken = refreshToken,
        idToken = idToken,
        expiryDate = expiryDate,
        permittedLanguageTags = permittedLanguageTags,
        permittedVersionIds = permittedVersionIds,
        excludedVersionIds = excludedVersionIds,
        grantedPermissions = grantedPermissions,
        isSignInEnabled = isSignInEnabled,
        appName = appName,
        signInPromptMessage = signInPromptMessage,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(config(), config())
        assertEquals(config().hashCode(), config().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            config(appKey = null),
            config(authCallback = "other"),
            config(apiHost = "other"),
            config(hostEnv = null),
            config(installId = null),
            config(accessToken = null),
            config(refreshToken = null),
            config(idToken = null),
            config(expiryDate = null),
            config(permittedLanguageTags = null),
            config(permittedVersionIds = null),
            config(excludedVersionIds = emptySet()),
            config(grantedPermissions = emptySet()),
            config(isSignInEnabled = false),
            config(appName = null),
            config(signInPromptMessage = null),
        ).forEach {
            assertNotEquals(config(), it)
            assertNotEquals(config().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString masks app key and tokens`() {
        assertEquals(
            "Config(appKey=<redacted>, authCallback=authCallback, apiHost=apiHost, hostEnv=hostEnv, " +
                "installId=installId, accessToken=<redacted>, refreshToken=<redacted>, idToken=<redacted>, " +
                "expiryDate=${Date(1000L)}, permittedLanguageTags=[en], permittedVersionIds=[1], " +
                "excludedVersionIds=[2], grantedPermissions=[${SignInWithYouVersionPermission.EMAIL}], " +
                "isSignInEnabled=true, appName=appName, signInPromptMessage=message)",
            config().toString(),
        )
    }

    @Test
    fun `test toString shows absent tokens as null`() {
        val text = config(accessToken = null, refreshToken = null, idToken = null).toString()
        assertTrue(text.contains("accessToken=null"))
        assertTrue(text.contains("refreshToken=null"))
        assertTrue(text.contains("idToken=null"))
    }
}
