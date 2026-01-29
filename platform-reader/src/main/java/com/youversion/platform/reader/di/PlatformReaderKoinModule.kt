package com.youversion.platform.reader.di

import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.domain.BibleReaderGlobalState
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.UserSettingsRepository
import com.youversion.platform.reader.screens.languages.LanguagesViewModel
import com.youversion.platform.reader.screens.versions.VersionsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val PlatformReaderKoinModule =
    module {
        // Global Reader State
        singleOf(::BibleReaderGlobalState)

        // Repositories
        factory { BibleVersionRepository(context = get()) }
        singleOf(::BibleReaderRepository)
        factoryOf(::UserSettingsRepository)

        // View Models
        factory { params ->
            BibleReaderViewModel(
                bibleReference = params[0],
                fontDefinitionProvider = params[1],
                bibleVersionRepository = get(),
                bibleReaderRepository = get(),
                userSettingsRepository = get(),
            )
        }

        factoryOf(::VersionsViewModel)
        factoryOf(::LanguagesViewModel)
    }
