package com.youversion.platform.core.users.model

/**
 * The raw values of the permissions an app can ask the user to grant, for sign-in, data exchange and
 * [com.youversion.platform.core.api.YouVersionApi.hasPermission]. Permissions are plain strings, so an optional
 * permission the platform adds later can be requested before this SDK names it. Sign-in sends [OPENID], [PROFILE],
 * [EMAIL], [READ_NOTES] and [WRITE_NOTES] as OAuth scopes and every other value as an optional permission, so a new
 * OAuth scope needs an SDK update.
 */
public object YouVersionPermissions {
    /** The user's identity. Always requested when signing in. */
    public const val OPENID: String = "openid"

    /** The user's name and profile picture. */
    public const val PROFILE: String = "profile"

    /** The user's email address. */
    public const val EMAIL: String = "email"

    /** Reading and writing the user's Bible highlights. */
    public const val HIGHLIGHTS: String = "highlights"

    /** Reading the user's notes. */
    public const val READ_NOTES: String = "read_notes"

    /** Creating, updating and deleting the user's notes. */
    public const val WRITE_NOTES: String = "write_notes"
}
