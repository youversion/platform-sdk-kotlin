package com.youversion.platform.ui.dataexchange

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.dataexchange.api.DataExchangeApi
import com.youversion.platform.core.dataexchange.models.DataExchangeToken
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DataExchangeHandlerTest {
    @Test
    fun `a failed token request is reported as not started rather than thrown`() =
        runTest {
            mockkObject(YouVersionApi)
            try {
                val api = mockk<DataExchangeApi>()
                coEvery { api.dataExchangeToken(any()) } throws
                    YouVersionNetworkException(YouVersionNetworkException.Reason.NOT_PERMITTED)
                every { YouVersionApi.dataExchange } returns api

                val handler = DataExchangeHandler(mockk(relaxed = true))
                val result = handler.requestDataExchange(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

                assertEquals(DataExchangeStatus.NotStarted, result.status)
                assertFalse(result.isGranted)
            } finally {
                unmockkObject(YouVersionApi)
            }
        }

    @Test
    fun `an unreadable callback is reported as cancelled rather than crashing the dispatch`() =
        runTest {
            mockkObject(YouVersionApi)
            try {
                YouVersionPlatformConfiguration.configure(
                    context = ApplicationProvider.getApplicationContext<Context>(),
                    appKey = "test",
                )
                val api = mockk<DataExchangeApi>()
                coEvery { api.dataExchangeToken(any()) } returns DataExchangeToken(token = "exchange-token")
                every { YouVersionApi.dataExchange } returns api

                val registry = ImmediateCallbackRegistry("youversionauth:callback?data_exchange_status=granted")
                val result =
                    DataExchangeHandler(registry).requestDataExchange(
                        setOf(SignInWithYouVersionPermission.HIGHLIGHTS),
                    )

                assertEquals(DataExchangeStatus.Cancelled, result.status)
                assertFalse(result.grants(SignInWithYouVersionPermission.HIGHLIGHTS))
            } finally {
                unmockkObject(YouVersionApi)
                YouVersionPlatformConfiguration.clearAuthData()
                PlatformKoinGraph.stop()
            }
        }

    @Test
    fun `a granted permission is persisted while the callback is still being dispatched`() =
        runTest {
            mockkObject(YouVersionApi)
            try {
                YouVersionPlatformConfiguration.configure(
                    context = ApplicationProvider.getApplicationContext<Context>(),
                    appKey = "test",
                )
                val api = mockk<DataExchangeApi>()
                coEvery { api.dataExchangeToken(any()) } returns DataExchangeToken(token = "exchange-token")
                every { YouVersionApi.dataExchange } returns api

                val registry =
                    ImmediateCallbackRegistry(
                        "youversionauth://callback?data_exchange_status=granted&granted_permissions=highlights",
                    )
                val result =
                    DataExchangeHandler(registry).requestDataExchange(
                        setOf(SignInWithYouVersionPermission.HIGHLIGHTS),
                    )

                assertTrue(result.grants(SignInWithYouVersionPermission.HIGHLIGHTS))
                assertTrue(registry.isHighlightsPersistedDuringDispatch)
            } finally {
                unmockkObject(YouVersionApi)
                YouVersionPlatformConfiguration.clearAuthData()
                PlatformKoinGraph.stop()
            }
        }

    @Test
    fun `a grant is not persisted when a different user signed in while the browser was open`() =
        runTest {
            mockkObject(YouVersionApi)
            try {
                YouVersionPlatformConfiguration.configure(
                    context = ApplicationProvider.getApplicationContext<Context>(),
                    appKey = "test",
                    accessToken = "userAAccessToken",
                    refreshToken = "userARefreshToken",
                    expiryDate = Date(),
                )
                val api = mockk<DataExchangeApi>()
                coEvery { api.dataExchangeToken(any()) } returns DataExchangeToken(token = "exchange-token")
                every { YouVersionApi.dataExchange } returns api

                val registry =
                    ImmediateCallbackRegistry(
                        callbackUrl =
                            "youversionauth://callback?data_exchange_status=granted&granted_permissions=highlights",
                        beforeDispatch = {
                            YouVersionPlatformConfiguration.saveAuthData(
                                accessToken = "userBAccessToken",
                                refreshToken = "userBRefreshToken",
                                idToken = null,
                                expiryDate = Date(),
                            )
                        },
                    )
                val result =
                    DataExchangeHandler(registry).requestDataExchange(
                        setOf(SignInWithYouVersionPermission.HIGHLIGHTS),
                    )

                // The user who answered the prompt granted it; the user now signed in never did.
                assertTrue(result.grants(SignInWithYouVersionPermission.HIGHLIGHTS))
                assertFalse(YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS))
            } finally {
                unmockkObject(YouVersionApi)
                YouVersionPlatformConfiguration.clearAuthData()
                PlatformKoinGraph.stop()
            }
        }

    @Test
    fun `a flow that could not be started never reports the browser as opened`() =
        runTest {
            mockkObject(YouVersionApi)
            try {
                val api = mockk<DataExchangeApi>()
                coEvery { api.dataExchangeToken(any()) } throws
                    YouVersionNetworkException(YouVersionNetworkException.Reason.NOT_PERMITTED)
                every { YouVersionApi.dataExchange } returns api

                var isBrowserOpened = false
                val result =
                    DataExchangeHandler(mockk(relaxed = true)).requestDataExchange(
                        permissions = setOf(SignInWithYouVersionPermission.HIGHLIGHTS),
                        onBrowserOpened = { isBrowserOpened = true },
                    )

                assertEquals(DataExchangeStatus.NotStarted, result.status)
                assertFalse(isBrowserOpened)
            } finally {
                unmockkObject(YouVersionApi)
            }
        }

    @Test
    fun `a launched flow reports the browser as opened`() =
        runTest {
            mockkObject(YouVersionApi)
            try {
                YouVersionPlatformConfiguration.configure(
                    context = ApplicationProvider.getApplicationContext<Context>(),
                    appKey = "test",
                )
                val api = mockk<DataExchangeApi>()
                coEvery { api.dataExchangeToken(any()) } returns DataExchangeToken(token = "exchange-token")
                every { YouVersionApi.dataExchange } returns api

                var isBrowserOpened = false
                DataExchangeHandler(
                    ImmediateCallbackRegistry("youversionauth://callback?data_exchange_status=cancel"),
                ).requestDataExchange(
                    permissions = setOf(SignInWithYouVersionPermission.HIGHLIGHTS),
                    onBrowserOpened = { isBrowserOpened = true },
                )

                assertTrue(isBrowserOpened)
            } finally {
                unmockkObject(YouVersionApi)
                YouVersionPlatformConfiguration.clearAuthData()
                PlatformKoinGraph.stop()
            }
        }
}

/**
 * Delivers the callback the moment the flow launches, then records whether the grant was already persisted by the
 * time that dispatch returned.
 *
 * Android delivers an activity result before the host activity resumes, so a grant persisted during the dispatch is
 * readable by a caller that checks the permission on resume, and one persisted any later is not.
 */
private class ImmediateCallbackRegistry(
    private val callbackUrl: String,
    private val beforeDispatch: () -> Unit = {},
) : ActivityResultRegistry() {
    var isHighlightsPersistedDuringDispatch = false
        private set

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        beforeDispatch()
        dispatchResult(requestCode, Activity.RESULT_OK, Intent(Intent.ACTION_VIEW, callbackUrl.toUri()))
        isHighlightsPersistedDuringDispatch =
            YouVersionPlatformConfiguration.configState.value
                ?.grantedPermissions
                ?.contains(SignInWithYouVersionPermission.HIGHLIGHTS) == true
    }
}
