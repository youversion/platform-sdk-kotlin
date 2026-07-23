package com.youversion.platform.ui.dataexchange

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.dataexchange.api.DataExchangeApi
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
class DataExchangeHandlerTest {
    @Test
    fun `a failed token request is reported as cancelled rather than thrown`() =
        runTest {
            mockkObject(YouVersionApi)
            try {
                val api = mockk<DataExchangeApi>()
                coEvery { api.dataExchangeToken(any()) } throws
                    YouVersionNetworkException(YouVersionNetworkException.Reason.NOT_PERMITTED)
                every { YouVersionApi.dataExchange } returns api

                val handler = DataExchangeHandler(mockk(relaxed = true))
                val result = handler.requestDataExchange(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))

                assertEquals(DataExchangeStatus.Cancelled, result.status)
                assertFalse(result.isGranted)
            } finally {
                unmockkObject(YouVersionApi)
            }
        }
}
