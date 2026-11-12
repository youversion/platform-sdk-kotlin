package com.youversion.platform.ui.dataexchange

import androidx.core.net.toUri
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class DataExchangeCallbackTest {
    @Test
    fun `reads a granted permission from the callback`() {
        val result =
            dataExchangeResult(
                "youversionauth://callback?data_exchange_status=granted&granted_permissions=highlights".toUri(),
            )

        assertTrue(result.isGranted)
        assertEquals(listOf(SignInWithYouVersionPermission.HIGHLIGHTS), result.grantedPermissions)
    }

    @Test
    fun `reads every granted permission when several are returned`() {
        val result =
            dataExchangeResult(
                (
                    "youversionauth://callback?data_exchange_status=granted" +
                        "&granted_permissions=highlights&granted_permissions=profile"
                ).toUri(),
            )

        assertEquals(
            listOf(
                SignInWithYouVersionPermission.HIGHLIGHTS,
                SignInWithYouVersionPermission.PROFILE,
            ),
            result.grantedPermissions,
        )
    }

    @Test
    fun `ignores a granted permission this version does not recognize`() {
        val result =
            dataExchangeResult(
                (
                    "youversionauth://callback?data_exchange_status=granted" +
                        "&granted_permissions=highlights&granted_permissions=notes"
                ).toUri(),
            )

        assertEquals(listOf(SignInWithYouVersionPermission.HIGHLIGHTS), result.grantedPermissions)
    }

    @Test
    fun `treats a cancelled callback as not granted`() {
        val result = dataExchangeResult("youversionauth://callback?data_exchange_status=cancel".toUri())

        assertEquals(DataExchangeStatus.Cancelled, result.status)
        assertFalse(result.isGranted)
    }

    @Test
    fun `treats a callback with no status as not granted`() {
        val result = dataExchangeResult("youversionauth://callback".toUri())

        assertEquals(DataExchangeStatus.Missing, result.status)
        assertFalse(result.isGranted)
    }

    @Test
    fun `preserves a status it does not recognize rather than coercing it`() {
        val result =
            dataExchangeResult("youversionauth://callback?data_exchange_status=needs_review".toUri())

        assertEquals(DataExchangeStatus.Unknown("needs_review"), result.status)
        assertFalse(result.isGranted)
    }

    @Test
    fun `is not granted when the status is granted but highlights was withheld`() {
        val result =
            dataExchangeResult(
                "youversionauth://callback?data_exchange_status=granted&granted_permissions=profile".toUri(),
            )

        assertTrue(result.isGranted)
        assertFalse(result.grantedPermissions.contains(SignInWithYouVersionPermission.HIGHLIGHTS))
    }

    @Test
    fun `only a highlights grant reports grants of highlights`() {
        fun grantsHighlights(callback: String) =
            dataExchangeResult(callback.toUri()).grants(SignInWithYouVersionPermission.HIGHLIGHTS)

        // The one outcome the reader acts on.
        assertTrue(
            grantsHighlights("youversionauth://callback?data_exchange_status=granted&granted_permissions=highlights"),
        )

        // Every non-grant outcome converges on false.
        assertFalse(grantsHighlights("youversionauth://callback?data_exchange_status=cancel"))
        assertFalse(grantsHighlights("youversionauth://callback"))
        assertFalse(grantsHighlights("youversionauth://callback?data_exchange_status=needs_review"))
        assertFalse(
            grantsHighlights("youversionauth://callback?data_exchange_status=granted&granted_permissions=profile"),
        )
    }

    @Test
    fun `results with equal values are equal`() {
        val permissions = listOf(SignInWithYouVersionPermission.HIGHLIGHTS)
        val first = DataExchangeResult(DataExchangeStatus.Granted, permissions)
        val second = DataExchangeResult(DataExchangeStatus.Granted, permissions)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `results differing in any value are not equal`() {
        val base = DataExchangeResult(DataExchangeStatus.Granted, listOf(SignInWithYouVersionPermission.HIGHLIGHTS))
        assertNotEquals(base, DataExchangeResult(DataExchangeStatus.Cancelled, base.grantedPermissions))
        assertNotEquals(base, DataExchangeResult(DataExchangeStatus.Granted, emptyList()))
    }

    @Test
    fun `result toString matches the existing format`() {
        val permissions = listOf(SignInWithYouVersionPermission.HIGHLIGHTS)
        assertEquals(
            "DataExchangeResult(status=Granted, grantedPermissions=$permissions)",
            DataExchangeResult(DataExchangeStatus.Granted, permissions).toString(),
        )
    }

    @Test
    fun `unknown statuses compare by raw value`() {
        assertEquals(DataExchangeStatus.Unknown("a"), DataExchangeStatus.Unknown("a"))
        assertEquals(DataExchangeStatus.Unknown("a").hashCode(), DataExchangeStatus.Unknown("a").hashCode())
        assertNotEquals(DataExchangeStatus.Unknown("a"), DataExchangeStatus.Unknown("b"))
        assertEquals("Unknown(rawValue=a)", DataExchangeStatus.Unknown("a").toString())
    }
}
