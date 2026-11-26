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
 * @property grantedPermissionValues The raw values of the permissions the user granted, such as
 *           [com.youversion.platform.core.users.model.YouVersionPermissions.HIGHLIGHTS].
 */
public class DataExchangeResult
    @PlatformInternalApi
    constructor(
        public val status: DataExchangeStatus,
        public val grantedPermissionValues: List<String>,
    ) {
        /** The granted permissions this SDK version names, without the values it does not. */
        @Deprecated("Use grantedPermissionValues instead.", ReplaceWith("grantedPermissionValues"))
        public val grantedPermissions: List<SignInWithYouVersionPermission>
            get() = grantedPermissionValues.mapNotNull { SignInWithYouVersionPermission.fromRawValue(it) }

        /**
         * Whether the user completed the flow. This says nothing about *which* permissions they granted — check
         * [grantedPermissionValues] for the one being asked about, since a user can complete the flow while withholding it.
         */
        public val isGranted: Boolean
            get() = status == DataExchangeStatus.Granted

        /**
         * Whether the user granted [permission]. This is the single question the reader flow acts on: a cancellation, a
         * missing or unrecognized status, and a completed flow that withheld [permission] all answer false, so every
         * non-grant outcome converges here.
         *
         * @param permission The raw value of the permission to check, such as
         *        [com.youversion.platform.core.users.model.YouVersionPermissions.HIGHLIGHTS].
         */
        public fun grants(permission: String): Boolean = grantedPermissionValues.contains(permission)

        /**
         * Whether the user granted [permission].
         *
         * @param permission The permission to check.
         */
        @Deprecated(
            "Use grants with a raw String permission value instead.",
            ReplaceWith("grants(permission.rawValue)"),
        )
        public fun grants(permission: SignInWithYouVersionPermission): Boolean = grants(permission.rawValue)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DataExchangeResult) return false
            return status == other.status && grantedPermissionValues == other.grantedPermissionValues
        }

        override fun hashCode(): Int {
            var result = status.hashCode()
            result = 31 * result + grantedPermissionValues.hashCode()
            return result
        }

        override fun toString(): String =
            "DataExchangeResult(status=$status, grantedPermissionValues=$grantedPermissionValues)"
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
        grantedPermissionValues =
            uri
                .getQueryParameters(GRANTED_PERMISSIONS_PARAMETER)
                .filter { it.isNotEmpty() },
    )
