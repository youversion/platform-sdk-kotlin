package com.youversion.platform.core

import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.users.domain.SessionRepository
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import org.koin.test.inject
import java.util.Date
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class YouVersionPlatformConfigurationTest : YouVersionPlatformTest {
    val storage: Storage by inject()
    val sessionRepository: SessionRepository by inject()

    @BeforeTest
    fun setup() = startYouVersionPlatformTest()

    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

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
}
