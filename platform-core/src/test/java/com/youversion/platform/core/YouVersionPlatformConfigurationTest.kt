package com.youversion.platform.core

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.users.domain.SessionRepository
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.utilities.exceptions.YouVersionNotConfiguredException
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import org.koin.test.inject
import java.util.Base64
import java.util.Date
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouVersionPlatformConfigurationTest : YouVersionPlatformTest {
    val storage: Storage by inject()
    val sessionRepository: SessionRepository by inject()

    @BeforeTest
    fun setup() = startYouVersionPlatformTest()

    @AfterTest
    fun teardown() {
        YouVersionPlatformConfiguration.reset()
        stopYouVersionPlatformTest()
    }

    @Test
    fun `configure without saved values`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")

            assertEquals("appKey", appKey)
            assertEquals("youversionauth://callback", authCallback)
            assertNull(accessToken)
            assertEquals("api.youversion.com", apiHost)
            assertNull(hostEnv)
            assertNotNull(installId)
            assertEquals(sessionRepository.installId, installId)
        }
    }

    @Test
    fun `configure sets given values`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                authCallback = "auth_callback",
                accessToken = "token",
                apiHost = "example.com",
                hostEnv = "prod",
            )

            assertEquals("appKey", appKey)
            assertEquals("auth_callback", authCallback)
            assertEquals("token", accessToken)
            assertEquals("example.com", apiHost)
            assertEquals("prod", hostEnv)
            assertNotNull(installId)
            assertEquals(sessionRepository.installId, installId)
        }
    }

    @Test
    fun `permittedLanguageTags and permittedVersionIds default to null`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")

            assertNull(permittedLanguageTags)
            assertNull(permittedVersionIds)
        }
    }

    @Test
    fun `configure sets permittedLanguageTags and permittedVersionIds when provided`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                permittedLanguageTags = setOf("en", "es"),
                permittedVersionIds = setOf(111, 1588),
            )

            assertEquals(setOf("en", "es"), permittedLanguageTags)
            assertEquals(setOf(111, 1588), permittedVersionIds)
        }
    }

    @Test
    fun `reconfigure clears permittedLanguageTags and permittedVersionIds when omitted`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                permittedLanguageTags = setOf("en"),
                permittedVersionIds = setOf(111),
            )
            assertEquals(setOf("en"), permittedLanguageTags)

            configure(appKey = "appKey")
            assertNull(permittedLanguageTags)
            assertNull(permittedVersionIds)
        }
    }

    @Test
    fun `configure with saved values`() {
        sessionRepository.accessToken = "stored_token"
        sessionRepository.setInstallId("existing_id")

        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")

            assertEquals("stored_token", accessToken)
            assertEquals("existing_id", installId)
        }
    }

    @Test
    fun saveAuthData() {
        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                idToken = "idToken",
                expiryDate = Date(),
            )
            assertEquals("accessToken", accessToken)
            assertNull(sessionRepository.accessToken)

            val newDate = Date()

            saveAuthData(
                accessToken = "newToken",
                refreshToken = "newRefreshToken",
                idToken = "newIdToken",
                expiryDate = newDate,
                persist = false,
            )
            assertEquals("newToken", accessToken)
            assertEquals("newRefreshToken", refreshToken)
            assertEquals("newIdToken", idToken)
            assertEquals(newDate, expiryDate)

            assertNull(sessionRepository.accessToken)
            assertNull(sessionRepository.refreshToken)
            assertNull(sessionRepository.expiryDate)

            saveAuthData(
                accessToken = "persistedToken",
                refreshToken = "persistedRefreshToken",
                idToken = "persistedIdToken",
                expiryDate = newDate,
            )
            assertEquals("persistedToken", accessToken)
            assertEquals("persistedToken", sessionRepository.accessToken)

            assertEquals("persistedRefreshToken", refreshToken)
            assertEquals("persistedRefreshToken", sessionRepository.refreshToken)

            assertEquals("persistedIdToken", idToken)
            assertEquals("persistedIdToken", sessionRepository.idToken)

            assertEquals(newDate, expiryDate)
            assertEquals(newDate, sessionRepository.expiryDate)
        }
    }

    @Test
    fun `setApiHost changes the api host`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey", accessToken = "accessToken", apiHost = "apiHost")
            assertEquals("apiHost", apiHost)

            setApiHost(apiHost = "newApiHost")
            assertEquals("newApiHost", apiHost)
        }
    }

    @Test
    fun `clearAuthData nulls all auth fields`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                idToken = "idToken",
                expiryDate = Date(),
            )

            saveAuthData(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                idToken = "idToken",
                expiryDate = Date(),
            )

            clearAuthData()

            assertNull(accessToken)
            assertNull(refreshToken)
            assertNull(idToken)
            assertNull(expiryDate)
            assertNull(sessionRepository.accessToken)
            assertNull(sessionRepository.refreshToken)
            assertNull(sessionRepository.idToken)
            assertNull(sessionRepository.expiryDate)
        }
    }

    @Test
    fun `saveAuthData throws when not configured`() {
        assertFailsWith<YouVersionNotConfiguredException> {
            YouVersionPlatformConfiguration.saveAuthData(
                accessToken = "token",
                refreshToken = "refresh",
                idToken = "id",
                expiryDate = Date(),
            )
        }
    }

    @Test
    fun `setApiHost throws when not configured`() {
        assertFailsWith<YouVersionNotConfiguredException> {
            YouVersionPlatformConfiguration.setApiHost("example.com")
        }
    }

    @Test
    fun `clearAuthData throws when not configured`() {
        assertFailsWith<YouVersionNotConfiguredException> {
            YouVersionPlatformConfiguration.clearAuthData()
        }
    }

    @Test
    fun `isSignedIn returns true when accessToken is set`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey", accessToken = "token")
            assertTrue(isSignedIn)
        }
    }

    @Test
    fun `isSignedIn returns false when accessToken is null`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")
            assertFalse(isSignedIn)
        }
    }

    @Test
    fun `configure with saved values restores all tokens`() {
        val savedDate = Date()
        sessionRepository.accessToken = "stored_access"
        sessionRepository.refreshToken = "stored_refresh"
        sessionRepository.idToken = "stored_id"
        sessionRepository.expiryDate = savedDate
        sessionRepository.setInstallId("stored_install")

        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")

            assertEquals("stored_access", accessToken)
            assertEquals("stored_refresh", refreshToken)
            assertEquals("stored_id", idToken)
            assertEquals(savedDate, expiryDate)
            assertEquals("stored_install", installId)
        }
    }

    @Test
    fun `configure with explicit tokens takes precedence over saved values`() {
        sessionRepository.accessToken = "stored_access"
        sessionRepository.refreshToken = "stored_refresh"
        sessionRepository.idToken = "stored_id"
        sessionRepository.expiryDate = Date(1000L)

        val explicitDate = Date(2000L)

        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                accessToken = "explicit_access",
                refreshToken = "explicit_refresh",
                idToken = "explicit_id",
                expiryDate = explicitDate,
            )

            assertEquals("explicit_access", accessToken)
            assertEquals("explicit_refresh", refreshToken)
            assertEquals("explicit_id", idToken)
            assertEquals(explicitDate, expiryDate)
        }
    }

    @Test
    fun `reconfigure resets state`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                accessToken = "token",
                refreshToken = "refresh",
                idToken = "id",
                expiryDate = Date(),
            )
            assertTrue(isSignedIn)

            configure(appKey = "appKey")

            assertNull(accessToken)
            assertNull(refreshToken)
            assertNull(idToken)
            assertNull(expiryDate)
            assertFalse(isSignedIn)
        }
    }

    @Test
    fun `grantedPermissions is empty when nothing has been granted`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")

            assertEquals(emptySet(), grantedPermissions)
            assertFalse(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
        }
    }

    @Test
    fun `granted permissions survive a relaunch`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")
            saveGrantedPermissions(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

            configure(appKey = "appKey")

            assertTrue(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
        }
    }

    @Test
    fun `saveGrantedPermissions unions with previously granted permissions`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")

            saveGrantedPermissions(setOf(SignInWithYouVersionPermission.PROFILE))
            saveGrantedPermissions(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

            assertTrue(YouVersionApi.hasPermission(SignInWithYouVersionPermission.PROFILE))
            assertTrue(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
        }
    }

    @Test
    fun `configure with tokens naming a different user drops the stored session`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")
            saveAuthData(
                accessToken = "userAAccessToken",
                refreshToken = "userARefreshToken",
                idToken = idTokenFor("userA"),
                expiryDate = Date(),
            )
            saveGrantedPermissions(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

            // A host managing its own tokens reconfigures as another user.
            configure(appKey = "appKey", accessToken = "userBAccessToken")

            assertFalse(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
            // Mixing in user A's tokens would report user A while requests carried user B's.
            assertEquals("userBAccessToken", accessToken)
            assertNull(refreshToken)
            assertNull(idToken)
        }
    }

    @Test
    fun `configure with the stored access token keeps the rest of the stored session`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")
            saveAuthData(
                accessToken = "userAAccessToken",
                refreshToken = "userARefreshToken",
                idToken = idTokenFor("userA"),
                expiryDate = Date(),
            )
            saveGrantedPermissions(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

            configure(appKey = "appKey", accessToken = "userAAccessToken")

            assertTrue(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
            assertEquals("userARefreshToken", refreshToken)
        }
    }

    @Test
    fun `granted permissions dropped by a reconfiguration are not restored by a later saveAuthData`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")
            saveAuthData(
                accessToken = "userAAccessToken",
                refreshToken = "userARefreshToken",
                idToken = idTokenFor("userA"),
                expiryDate = Date(),
            )
            saveGrantedPermissions(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

            configure(appKey = "appKey", accessToken = "userBAccessToken")
            // The same session the reconfiguration established, so this keeps whatever permissions storage holds.
            saveAuthData(
                accessToken = "userBAccessToken",
                refreshToken = null,
                idToken = null,
                expiryDate = Date(),
            )

            assertFalse(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
        }
    }

    @Test
    fun `saveAuthData preserves granted permissions across a token refresh`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                idToken = idTokenFor("userA"),
                expiryDate = Date(),
            )
            saveGrantedPermissions(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

            saveAuthData(
                accessToken = "refreshedToken",
                refreshToken = "refreshedRefreshToken",
                idToken = idTokenFor("userA"),
                expiryDate = Date(),
            )

            assertTrue(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))

            configure(appKey = "appKey")
            assertTrue(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
        }
    }

    @Test
    fun `saveAuthData drops granted permissions when the tokens name a different user`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                idToken = idTokenFor("userA"),
                expiryDate = Date(),
            )
            saveGrantedPermissions(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

            saveAuthData(
                accessToken = "userBAccessToken",
                refreshToken = "userBRefreshToken",
                idToken = idTokenFor("userB"),
                expiryDate = Date(),
            )

            assertEquals(emptySet(), grantedPermissions)
            assertFalse(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))

            configure(appKey = "appKey")
            assertFalse(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
        }
    }

    @Test
    fun `saveAuthData drops granted permissions when an unnamed user is replaced`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                expiryDate = Date(),
            )
            saveGrantedPermissions(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

            saveAuthData(
                accessToken = "userBAccessToken",
                refreshToken = "userBRefreshToken",
                idToken = null,
                expiryDate = Date(),
            )

            assertFalse(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
        }
    }

    @Test
    fun `saveAuthData preserves granted permissions when an unnamed user refreshes`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                expiryDate = Date(),
            )
            saveGrantedPermissions(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

            saveAuthData(
                accessToken = "refreshedAccessToken",
                refreshToken = "refreshToken",
                idToken = null,
                expiryDate = Date(),
            )

            assertTrue(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
        }
    }

    @Test
    fun `clearAuthData clears granted permissions`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")
            saveGrantedPermissions(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

            clearAuthData()

            assertEquals(emptySet(), grantedPermissions)
            assertFalse(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))

            configure(appKey = "appKey")
            assertFalse(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
        }
    }

    @Test
    fun `an unrecognized stored permission is reported as not granted`() {
        storage.putString(SessionRepository.KEY_GRANTED_PERMISSIONS, "highlights,notes")

        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")

            assertEquals(setOf(SignInWithYouVersionPermission.HIGHLIGHTS), grantedPermissions)
            assertTrue(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
        }
    }

    @Test
    fun `configState emits updated config`() {
        with(YouVersionPlatformConfiguration) {
            assertNull(configState.value)

            configure(appKey = "appKey")
            assertNotNull(configState.value)
            assertEquals("appKey", configState.value?.appKey)
            assertNull(configState.value?.accessToken)

            saveAuthData(
                accessToken = "newToken",
                refreshToken = "newRefresh",
                idToken = "newId",
                expiryDate = Date(),
                persist = false,
            )
            assertEquals("newToken", configState.value?.accessToken)
            assertEquals("newRefresh", configState.value?.refreshToken)
            assertEquals("newId", configState.value?.idToken)
        }
    }

    private fun idTokenFor(subject: String): String {
        val payload =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString("""{"sub":"$subject"}""".toByteArray())
        return "header.$payload.signature"
    }
}
