package com.youversion.platform.core.di

import org.koin.core.KoinApplication
import org.koin.core.context.KoinContext
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

@PlatformInternalApi
public object PlatformKoinGraph {
    public val koinApplication: KoinApplication
        get() = PlatformKoinContext.koinApplication

    public fun start(modules: List<Module>) {
        PlatformKoinContext
            .startKoin(
                koinApplication {
                    modules(modules.toList())
                },
            )
    }

    public fun stop(): Unit = PlatformKoinContext.stopKoin()

    internal fun getContext(): KoinContext = PlatformKoinContext
}
