package com.youversion.platform.core

import android.app.Application
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class YouVersionPlatformConfigurationContextTest {
    private lateinit var context: Application

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun teardown() {
        YouVersionPlatformConfiguration.reset()
        PlatformKoinGraph.stop()
    }

    @Test
    fun `configure with context initializes SDK`() {
        with(YouVersionPlatformConfiguration) {
            configure(context = context, appKey = "appKey")

            assertEquals("appKey", appKey)
            assertEquals("youversionauth://callback", authCallback)
            assertEquals("api.youversion.com", apiHost)
            assertNull(hostEnv)
            assertNotNull(installId)
        }
    }

    @Test
    fun `configure with context and all parameters`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                context = context,
                appKey = "appKey",
                authCallback = "custom://callback",
                accessToken = "token",
                refreshToken = "refresh",
                idToken = "id",
                expiryDate = Date(1000L),
                apiHost = "custom.api.com",
                hostEnv = "staging",
            )

            assertEquals("appKey", appKey)
            assertEquals("custom://callback", authCallback)
            assertEquals("token", accessToken)
            assertEquals("refresh", refreshToken)
            assertEquals("id", idToken)
            assertEquals(Date(1000L), expiryDate)
            assertEquals("custom.api.com", apiHost)
            assertEquals("staging", hostEnv)
            assertNotNull(installId)
        }
    }

    @Test
    fun `reconfigure with context resets and reinitializes`() {
        with(YouVersionPlatformConfiguration) {
            configure(
                context = context,
                appKey = "firstKey",
                accessToken = "firstToken",
            )
            assertEquals("firstKey", appKey)
            assertEquals("firstToken", accessToken)

            configure(
                context = context,
                appKey = "secondKey",
            )
            assertEquals("secondKey", appKey)
            assertNull(accessToken)
        }
    }

    @Test
    fun `configure with context then save and clear auth data`() {
        with(YouVersionPlatformConfiguration) {
            configure(context = context, appKey = "appKey")
            assertFalse(isSignedIn)

            val expiryDate = Date()
            saveAuthData(
                accessToken = "token",
                refreshToken = "refresh",
                idToken = "id",
                expiryDate = expiryDate,
            )
            assertTrue(isSignedIn)
            assertEquals("token", accessToken)

            val sessionRepository = PlatformCoreKoinComponent.sessionRepository
            assertEquals("token", sessionRepository.accessToken)
            assertEquals("refresh", sessionRepository.refreshToken)
            assertEquals("id", sessionRepository.idToken)
            assertEquals(expiryDate, sessionRepository.expiryDate)

            clearAuthData()
            assertFalse(isSignedIn)
            assertNull(accessToken)
            assertNull(sessionRepository.accessToken)
            assertNull(sessionRepository.refreshToken)
            assertNull(sessionRepository.idToken)
            assertNull(sessionRepository.expiryDate)
        }
    }
}
