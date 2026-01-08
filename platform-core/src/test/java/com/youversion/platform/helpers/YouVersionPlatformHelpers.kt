package com.youversion.platform.helpers

import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.utilities.koin.PlatformCoreDomainKoinModule
import com.youversion.platform.core.utilities.koin.startCore
import com.youversion.platform.foundation.PlatformKoinGraph
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import org.koin.core.Koin
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest

interface YouVersionPlatformTest : KoinTest {
    override fun getKoin(): Koin = PlatformKoinGraph.getContext().get()
}

private fun youVersionTestKoinModule(engine: HttpClientEngine) =
    module {
        single { engine } bind HttpClientEngine::class
        singleOf(::TestStorage) bind Storage::class
    }

private val PlatformTestCacheKoinModule =
    module {
        single(named("memory")) { BibleVersionMemoryCache() } bind BibleVersionCache::class
        single(named("temporary")) { BibleVersionMemoryCache() } bind BibleVersionCache::class
        single(named("persistent")) { BibleVersionMemoryCache() } bind BibleVersionCache::class
    }

internal fun startYouVersionPlatformTest(engine: HttpClientEngine = MockEngine.create { addHandler { respondOk() } }) =
    PlatformKoinGraph.startCore(
        listOf(
            youVersionTestKoinModule(engine),
            PlatformTestCacheKoinModule,
            PlatformCoreDomainKoinModule,
        ),
    )

internal fun stopYouVersionPlatformTest() = PlatformKoinGraph.stop()
