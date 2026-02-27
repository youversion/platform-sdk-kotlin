package com.youversion.platform.core.di

import org.koin.core.context.KoinContext
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

@PlatformInternalApi
object PlatformKoinGraph {
    val koinApplication
        get() = PlatformKoinContext.koinApplication

    fun start(modules: List<Module>) {
        PlatformKoinContext
            .startKoin(
                koinApplication {
                    modules(modules.toList())
                },
            )
    }

    fun stop() = PlatformKoinContext.stopKoin()

    fun getContext(): KoinContext = PlatformKoinContext
}
