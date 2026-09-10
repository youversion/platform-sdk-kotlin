package com.youversion.platform.ui.views.components

import com.youversion.platform.core.di.PlatformInternalApi

@PlatformInternalApi
data class LanguageRowItem(
    val languageTag: String,
    val displayName: String,
    val localeDisplayName: String?,
)
