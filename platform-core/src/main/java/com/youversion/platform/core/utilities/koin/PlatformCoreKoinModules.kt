package com.youversion.platform.core.utilities.koin

import android.content.Context
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.bibles.data.BibleVersionPersistentCache
import com.youversion.platform.core.bibles.data.BibleVersionTemporaryCache
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.data.SharedPreferencesStorage
import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.core.users.domain.SessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpMessageBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

internal fun platformAppKoinModule(context: Context) =
    module {
        single { context.applicationContext }
        single { OkHttp.create() } bind HttpClientEngine::class
        factoryOf(::SharedPreferencesStorage) bind Storage::class
    }

internal val PlatformCoreCacheKoinModule =
    module {
        single(named("memory")) { BibleVersionMemoryCache() } bind BibleVersionCache::class
        factory(named("temporary")) { BibleVersionTemporaryCache(get()) } bind BibleVersionCache::class
        factory(named("persistent")) { BibleVersionPersistentCache(get()) } bind BibleVersionCache::class
    }

internal val PlatformCoreDomainKoinModule =
    module {
        factory {
            BibleVersionRepository(
                memoryCache = get(named("memory")),
                temporaryCache = get(named("temporary")),
                persistentCache = get(named("persistent")),
            )
        }

        factory {
            BibleChapterRepository(
                memoryCache = get(named("memory")),
                temporaryCache = get(named("temporary")),
                persistentCache = get(named("persistent")),
            )
        }

        factoryOf(::LanguageRepository)
        factoryOf(::SessionRepository)
    }

internal val PlatformCoreKoinModule =
    module {
        single {
            Json {
                ignoreUnknownKeys = true
            }
        }
        single {
            HttpClient(get()) {
                install(ContentNegotiation) { json(get()) }
                install(HttpCache) {
                    getOrNull<Context>()?.let {
                        privateStorage(FileStorage(it.cacheDir))
                    }
                }
                defaultRequest {
                    defaultRequestHeaders()
                }
                install(Logging) {
                    logger = logger()
                    level = LogLevel.INFO
                }
            }
        }
    }

private fun HttpMessageBuilder.defaultRequestHeaders(): HeadersBuilder =
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

private fun logger(): Logger =
    object : Logger {
        override fun log(message: String) {
            co.touchlab.kermit.Logger
                .i(message)
        }
    }
