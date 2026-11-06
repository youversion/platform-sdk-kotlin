package com.youversion.platform.ui.dataexchange

import android.net.Uri
import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission

/** How the data exchange permission flow ended. */
public sealed interface DataExchangeStatus {
    /** The user completed the flow. Which permissions they actually granted is reported separately. */
    public data object Granted : DataExchangeStatus

    /** The user backed out of the flow. */
    public data object Cancelled : DataExchangeStatus

    /** The callback carried no status at all. */
    public data object Missing : DataExchangeStatus

    /**
     * The flow could not be started, so the browser never opened and no callback or resume will follow. Unlike
     * [Cancelled], the user never saw the prompt.
     */
    public data object NotStarted : DataExchangeStatus

    /**
     * A status this version of the SDK does not recognize, kept as sent so it can be logged or handled by a caller
     * that does understand it.
     */
    public class Unknown
        @PlatformInternalApi
        constructor(
            public val rawValue: String,
        ) : DataExchangeStatus {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Unknown) return false
                return rawValue == other.rawValue
            }

            override fun hashCode(): Int = rawValue.hashCode()

            override fun toString(): String = "Unknown(rawValue=$rawValue)"
        }
}

/**
 * The outcome of the data exchange permission flow.
 *
 * @property status How the flow ended.
 * @property grantedPermissions The permissions the user granted. A value this SDK version does not recognize is
 *           omitted rather than reported, so callers only see permissions they can act on.
 */
public class DataExchangeResult
    @PlatformInternalApi
    constructor(
        public val status: DataExchangeStatus,
        public val grantedPermissions: List<SignInWithYouVersionPermission>,
    ) {
        /**
         * Whether the user completed the flow. This says nothing about *which* permissions they granted — check
         * [grantedPermissions] for the one being asked about, since a user can complete the flow while withholding it.
         */
        public val isGranted: Boolean
            get() = status == DataExchangeStatus.Granted

        /**
         * Whether the user granted [permission]. This is the single question the reader flow acts on: a cancellation, a
         * missing or unrecognized status, and a completed flow that withheld [permission] all answer false, so every
         * non-grant outcome converges here.
         *
         * @param permission The permission to check.
         */
        public fun grants(permission: SignInWithYouVersionPermission): Boolean = grantedPermissions.contains(permission)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DataExchangeResult) return false
            return status == other.status && grantedPermissions == other.grantedPermissions
        }

        override fun hashCode(): Int {
            var result = status.hashCode()
            result = 31 * result + grantedPermissions.hashCode()
            return result
        }

        override fun toString(): String = "DataExchangeResult(status=$status, grantedPermissions=$grantedPermissions)"
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
