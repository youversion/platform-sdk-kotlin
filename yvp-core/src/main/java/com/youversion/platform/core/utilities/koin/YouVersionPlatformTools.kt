package com.youversion.platform.core.utilities.koin

import org.koin.core.context.KoinContext

internal object YouVersionPlatformTools {
    fun defaultContext(): KoinContext = YouVersionPlatformContext
}
