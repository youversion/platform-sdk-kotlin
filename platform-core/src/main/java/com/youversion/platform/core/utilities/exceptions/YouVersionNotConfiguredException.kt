package com.youversion.platform.core.utilities.exceptions

/**
 * Thrown whenever code is accessed in the library that expected the library
 * to be configured first. Be sure to call `YouVersionPlatformConfiguration.configure`
 * first with you appId.
 */
class YouVersionNotConfiguredException : Exception("The library has not yet been configured")
