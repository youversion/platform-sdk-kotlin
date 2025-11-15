package com.youversion.platform.core.utilities.koin

import com.youversion.platform.core.utilities.dependencies.Store
import io.ktor.client.HttpClient
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

internal object YouVersionPlatformComponent : KoinComponent {
    override fun getKoin(): Koin = YouVersionPlatformTools.defaultContext().get()

    val httpClient: HttpClient
        get() = get()

    val store: Store
        get() = get()
}
