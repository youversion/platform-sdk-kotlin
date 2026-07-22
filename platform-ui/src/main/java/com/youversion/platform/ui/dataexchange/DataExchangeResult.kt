package com.youversion.platform.ui.dataexchange

import android.net.Uri
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission

/** How the data exchange permission flow ended. */
sealed interface DataExchangeStatus {
    /** The user completed the flow. Which permissions they actually granted is reported separately. */
    data object Granted : DataExchangeStatus

    /** The user backed out of the flow. */
    data object Cancelled : DataExchangeStatus

    /** The callback carried no status at all. */
    data object Missing : DataExchangeStatus

    /**
     * A status this version of the SDK does not recognize, kept as sent so it can be logged or handled by a caller
     * that does understand it.
     */
    data class Unknown(
        val rawValue: String,
    ) : DataExchangeStatus
}

/**
 * The outcome of the data exchange permission flow.
 *
 * @property status How the flow ended.
 * @property grantedPermissions The permissions the user granted. A value this SDK version does not recognize is
 *           omitted rather than reported, so callers only see permissions they can act on.
 */
data class DataExchangeResult(
    val status: DataExchangeStatus,
    val grantedPermissions: List<SignInWithYouVersionPermission>,
) {
    /**
     * Whether the user completed the flow. This says nothing about *which* permissions they granted — check
     * [grantedPermissions] for the one being asked about, since a user can complete the flow while withholding it.
     */
    val isGranted: Boolean
        get() = status == DataExchangeStatus.Granted
}

private const val STATUS_PARAMETER = "data_exchange_status"
private const val GRANTED_PERMISSIONS_PARAMETER = "granted_permissions"

/**
 * Reads the outcome of the permission flow out of its callback [uri].
 *
 * Kept separate from the browser flow that produces it so the parsing can be exercised on its own.
 */
internal fun dataExchangeResult(uri: Uri): DataExchangeResult =
    DataExchangeResult(
        status =
            when (val rawValue = uri.getQueryParameter(STATUS_PARAMETER)) {
                null, "" -> DataExchangeStatus.Missing
                "granted" -> DataExchangeStatus.Granted
                "cancel" -> DataExchangeStatus.Cancelled
                else -> DataExchangeStatus.Unknown(rawValue)
            },
        grantedPermissions =
            uri
                .getQueryParameters(GRANTED_PERMISSIONS_PARAMETER)
                .mapNotNull { SignInWithYouVersionPermission.fromRawValue(it) },
    )
