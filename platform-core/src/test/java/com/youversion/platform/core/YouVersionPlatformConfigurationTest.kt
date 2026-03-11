package com.youversion.platform.core

import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.users.domain.SessionRepository
import com.youversion.platform.core.utilities.exceptions.YouVersionNotConfiguredException
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import org.koin.test.inject
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
}
