package com.youversion.platform.core

import com.youversion.platform.core.utilities.dependencies.Store
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
    val store: Store by inject()

    @BeforeTest
    fun setup() = startYouVersionPlatformTest()

    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `configure without saved values`() {
        with(YouVersionPlatformConfiguration) {
            configure(appKey = "appKey")

            assertEquals("appKey", appKey)
            assertNull(accessToken)
            assertEquals("api.youversion.com", apiHost)
            assertNull(hostEnv)
            assertNotNull(installId)
            assertEquals(store.installId, installId)
        }
    }

    @Test
    fun `configure sets given values`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                appKey = "appKey",
                accessToken = "token",
                apiHost = "example.com",
                hostEnv = "prod",
            )

            assertEquals("appKey", appKey)
            assertEquals("token", accessToken)
            assertEquals("example.com", apiHost)
            assertEquals("prod", hostEnv)
            assertNotNull(installId)
            assertEquals(store.installId, installId)
        }
    }

    @Test
    fun `configure with saved values`() {
        store.accessToken = "stored_token"
        store.installId = "existing_id"

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
                expiryDate = Date(),
            )
            assertEquals("accessToken", accessToken)
            assertNull(store.accessToken)

            val newDate = Date()

            saveAuthData(
                accessToken = "newToken",
                refreshToken = "newRefreshToken",
                expiryDate = newDate,
                persist = false,
            )
            assertEquals("newToken", accessToken)
            assertEquals("newRefreshToken", refreshToken)
            assertEquals(newDate, expiryDate)

            assertNull(store.accessToken)
            assertNull(store.refreshToken)
            assertNull(store.expiryDate)

            saveAuthData(
                accessToken = "persistedToken",
                refreshToken = "persistedRefreshToken",
                expiryDate = newDate,
            )
            assertEquals("persistedToken", accessToken)
            assertEquals("persistedToken", store.accessToken)

            assertEquals("persistedRefreshToken", refreshToken)
            assertEquals("persistedRefreshToken", store.refreshToken)

            assertEquals(newDate, expiryDate)
            assertEquals(newDate, store.expiryDate)
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
