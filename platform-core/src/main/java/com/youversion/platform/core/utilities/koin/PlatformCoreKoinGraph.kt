package com.youversion.platform.core.utilities.koin

import android.content.Context
import com.youversion.platform.foundation.PlatformKoinGraph
import org.koin.core.module.Module

internal fun PlatformKoinGraph.startCore(context: Context) {
    startCore(
        listOf(
            platformAppKoinModule(context),
            PlatformCoreCacheKoinModule,
            PlatformCoreDomainKoinModule,
        ),
    )
}

internal fun PlatformKoinGraph.startCore(modules: List<Module>) {
    start(modules + listOf(PlatformCoreKoinModule))
}
