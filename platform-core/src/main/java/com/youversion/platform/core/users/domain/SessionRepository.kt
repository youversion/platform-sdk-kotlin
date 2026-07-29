package com.youversion.platform.core.users.domain

import com.youversion.platform.core.domain.Storage
import java.util.Date
import java.util.UUID

class SessionRepository(
    private val storage: Storage,
) {
    companion object {
        internal const val KEY_ACCESS_TOKEN = "YouVersionPlatformAccessToken"
        internal const val KEY_REFRESH_TOKEN = "YouVersionPlatformRefreshToken"
        internal const val KEY_ID_TOKEN = "YouVersionPlatformIDToken"
        internal const val KEY_EXPIRY_DATE = "YouVersionPlatformExpiryDate"
        internal const val KEY_INSTALL_ID = "YouVersionPlatformInstallID"
        internal const val KEY_GRANTED_PERMISSIONS = "YouVersionPlatformGrantedPermissions"

        private const val PERMISSION_SEPARATOR = ","
    }

    /**
     * Provides a unique installation ID
     */
    val installId: String
        get() =
            storage.getStringOrNull(KEY_INSTALL_ID)
                ?: UUID
                    .randomUUID()
                    .toString()
                    .also { setInstallId(it) }

    internal fun setInstallId(value: String) {
        storage.putString(KEY_INSTALL_ID, value)
    }

    var accessToken: String?
        get() = storage.getStringOrNull(KEY_ACCESS_TOKEN)
        set(value) = storage.putString(KEY_ACCESS_TOKEN, value)

    var refreshToken: String?
        get() = storage.getStringOrNull(KEY_REFRESH_TOKEN)
        set(value) = storage.putString(KEY_REFRESH_TOKEN, value)

    var idToken: String?
        get() = storage.getStringOrNull(KEY_ID_TOKEN)
        set(value) = storage.putString(KEY_ID_TOKEN, value)

    var expiryDate: Date?
        get() =
            storage
                .getStringOrNull(KEY_EXPIRY_DATE)
                ?.toLongOrNull()
                ?.let { Date(it) }
        set(value) = storage.putString(KEY_EXPIRY_DATE, value?.time?.toString())

    /**
     * The raw values of the permissions the signed-in user has granted.
     *
     * Stored as raw strings so that a stored value this SDK version does not recognize is ignored
     * when read back rather than failing to parse.
     */
    internal var grantedPermissionValues: Set<String>
        get() =
            storage
                .getStringOrNull(KEY_GRANTED_PERMISSIONS)
                ?.split(PERMISSION_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?.toSet()
                .orEmpty()
        set(value) =
            storage.putString(
                KEY_GRANTED_PERMISSIONS,
                value.sorted().joinToString(PERMISSION_SEPARATOR).takeIf { it.isNotEmpty() },
            )
}
