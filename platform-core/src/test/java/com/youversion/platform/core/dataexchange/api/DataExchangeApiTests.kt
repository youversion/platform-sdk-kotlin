package com.youversion.platform.core.dataexchange.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DataExchangeApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `requests a token for the permissions being asked for`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/data-exchange/token", request.url.encodedPath)
                assertEquals("app", request.url.parameters["app-key"])
                assertEquals("Bearer token", request.headers["Authorization"])
                assertEquals("application/json", request.body.contentType.toString())

                val decoded: JsonObject =
                    Json.decodeFromString(request.body.toByteArray().decodeToString())
                assertEquals(
                    listOf("highlights"),
                    decoded["permissions"]!!.jsonArray.map { it.jsonPrimitive.content },
                )

                respondJson("""{"token":"short-lived"}""", HttpStatusCode.Created)
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app", accessToken = "token")

            val token =
                YouVersionApi.dataExchange.dataExchangeToken(
                    setOf(SignInWithYouVersionPermission.HIGHLIGHTS),
                )

            assertEquals("short-lived", token.token)
        }

    @Test
    fun `reports a rejected token request as not permitted`() =
        runTest {
            MockEngine { respond("", HttpStatusCode.Unauthorized) }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app", accessToken = "token")

            val exception =
                assertFailsWith<YouVersionNetworkException> {
                    YouVersionApi.dataExchange.dataExchangeToken(
                        setOf(SignInWithYouVersionPermission.HIGHLIGHTS),
                    )
                }
            assertEquals(YouVersionNetworkException.Reason.NOT_PERMITTED, exception.reason)
        }

    @Test
    fun `reports an unexpected token response as a failed download`() =
        runTest {
            MockEngine { respond("", HttpStatusCode.InternalServerError) }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app", accessToken = "token")

            val exception =
                assertFailsWith<YouVersionNetworkException> {
                    YouVersionApi.dataExchange.dataExchangeToken(
                        setOf(SignInWithYouVersionPermission.HIGHLIGHTS),
                    )
                }
            assertEquals(YouVersionNetworkException.Reason.CANNOT_DOWNLOAD, exception.reason)
        }

    @Test
    fun `refuses to request a token for a signed-out user`() =
        runTest {
            MockEngine { respondJson("""{"token":"short-lived"}""", HttpStatusCode.Created) }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            val exception =
                assertFailsWith<YouVersionNetworkException> {
                    YouVersionApi.dataExchange.dataExchangeToken(
                        setOf(SignInWithYouVersionPermission.HIGHLIGHTS),
                    )
                }
            assertEquals(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION, exception.reason)
        }

    @Test
    fun `builds the permission page url from the token and app key`() {
        startYouVersionPlatformTest()
        YouVersionPlatformConfiguration.configure(appKey = "app")

        val url = DataExchangeEndpoints.dataExchangeUrl(token = "short-lived", appKey = "app")

        assertEquals(true, url.contains("/data-exchange"))
        assertEquals(true, url.contains("token=short-lived"))
        assertEquals(true, url.contains("app_key=app"))
    }
}
