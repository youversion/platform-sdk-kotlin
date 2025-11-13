package com.youversion.platform.core.utilities.koin

import com.youversion.platform.core.utilities.exceptions.YouVersionNotConfiguredException
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.KoinContext
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

internal object YouVersionPlatformContext : KoinContext {
    private var koin: Koin? = null

    override fun get(): Koin = koin ?: error("KoinApplication has not been started")

    override fun getOrNull(): Koin? = koin

    private fun register(koinApplication: KoinApplication) {
        if (koin != null) {
            throw YouVersionNotConfiguredException()
        }
        koin = koinApplication.koin
    }

    override fun stopKoin() =
        synchronized(this) {
            koin?.close()
            koin = null
        }

    override fun startKoin(koinApplication: KoinApplication): KoinApplication =
        synchronized(this) {
            register(koinApplication)
            koinApplication.createEagerInstances()
            return koinApplication
        }

    override fun startKoin(appDeclaration: KoinAppDeclaration): KoinApplication =
        synchronized(this) {
            val koinApplication = KoinApplication.init()
            register(koinApplication)
            appDeclaration(koinApplication)
            koinApplication.createEagerInstances()
            return koinApplication
        }

    override fun loadKoinModules(
        module: Module,
        createEagerInstances: Boolean,
    ) = synchronized(this) {
        get().loadModules(listOf(module), createEagerInstances = createEagerInstances)
    }

    override fun loadKoinModules(
        modules: List<Module>,
        createEagerInstances: Boolean,
    ) = synchronized(this) {
        get().loadModules(modules, createEagerInstances = createEagerInstances)
    }

    override fun unloadKoinModules(module: Module) =
        synchronized(this) {
            get().unloadModules(listOf(module))
        }

    override fun unloadKoinModules(modules: List<Module>) =
        synchronized(this) {
            get().unloadModules(modules)
        }
}
