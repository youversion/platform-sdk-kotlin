package com.youversion.platform.core.utilities.koin

import android.content.Context
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.utilities.dependencies.SharedPreferencesStore
import com.youversion.platform.core.utilities.dependencies.Store
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

internal fun startYouVersionPlatform(context: Context) =
    startYouVersionPlatform(
        listOf(
            module {
                single { context }
                single<HttpClientEngine> { OkHttp.create() }
                factory<Store> { SharedPreferencesStore(context = get()) }
            },
        ),
    )

internal fun startYouVersionPlatform(modules: List<Module>) {
    koinApplication {
        modules(modules.toList())
        modules(HttpClientModule)
    }.also {
        YouVersionPlatformTools
            .defaultContext()
            .startKoin(it)
    }
}

private val HttpClientModule =
    module {
        single { Json { ignoreUnknownKeys = true } }
        single {
            HttpClient(get()) {
                install(ContentNegotiation) { json(get()) }
                defaultRequest {
                    headers {
                        YouVersionPlatformConfiguration.appKey?.let {
                            append("x-yvp-app-key", it)
                        }

                        YouVersionPlatformConfiguration.installId?.let {
                            append("x-yvp-installation-id", it)
                        }

                        YouVersionPlatformConfiguration.accessToken?.let {
                            append("X-YV-LAT", it)
                        }
                    }
                }
                install(Logging) {
                    logger =
                        object : io.ktor.client.plugins.logging.Logger {
                            override fun log(message: String) {
                                co.touchlab.kermit.Logger
                                    .i(message)
                            }
                        }

                    // TODO: Ensure logging is set to `NONE` in production.
                    level = LogLevel.NONE
                }
            }
        }
    }
