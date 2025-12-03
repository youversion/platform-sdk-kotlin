package com.youversion.platform.core.users.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UsersEndpointsTests : YouVersionPlatformTest {
    @BeforeTest
    fun setup() = startYouVersionPlatformTest()

    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `test userUrl`() {
        assertEquals(
            "https://api.youversion.com/auth/me?lat=token",
            UsersEndpoints.userUrl("token").toString(),
        )
    }

    @Test
    fun `test authUrl`() {
        YouVersionPlatformConfiguration.configure(appKey = "app")

        val url =
            UsersEndpoints
                .authUrl(
                    appKey = "app",
                    requiredPermissions =
                        setOf(
                            SignInWithYouVersionPermission.OPENID,
                        ),
                    optionalPermissions =
                        setOf(
                            SignInWithYouVersionPermission.PROFILE,
                            SignInWithYouVersionPermission.EMAIL,
                        ),
                ).let { Url(urlString = it) }

        assertEquals(URLProtocol.HTTPS, url.protocol)
        assertEquals("api.youversion.com", url.host)
        assertEquals("/auth/login", url.encodedPath)
        assertEquals("app", url.parameters["app_id"])
        assertEquals("en", url.parameters["language"])
        assertEquals("openid", url.parameters["required_perms"])
        assertEquals("profile,email", url.parameters["opt_perms"])
        assertEquals(YouVersionPlatformConfiguration.installId, url.parameters["x-yvp-installation-id"])
    }
}
