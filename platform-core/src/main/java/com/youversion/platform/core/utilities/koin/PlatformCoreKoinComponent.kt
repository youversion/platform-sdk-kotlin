package com.youversion.platform.core.utilities.koin

import com.youversion.platform.core.users.domain.SessionRepository
import com.youversion.platform.foundation.PlatformKoinGraph
import io.ktor.client.HttpClient
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

internal object PlatformCoreKoinComponent : KoinComponent {
    override fun getKoin(): Koin = PlatformKoinGraph.getContext().get()

    val httpClient: HttpClient
        get() = get()

    val sessionRepository: SessionRepository
        get() = get()
}
