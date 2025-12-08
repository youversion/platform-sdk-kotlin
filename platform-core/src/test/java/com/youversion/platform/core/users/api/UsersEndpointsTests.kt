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
}
