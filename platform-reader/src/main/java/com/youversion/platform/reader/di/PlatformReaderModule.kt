package com.youversion.platform.reader.di

import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.UserSettingsRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val PlatformReaderModule =
    module {
        // Repositories
        factory { BibleVersionRepository(context = get()) }
        factoryOf(::BibleReaderRepository)
        factoryOf(::UserSettingsRepository)

        // View Models
        factory { params ->
            BibleReaderViewModel(
                bibleReference = params[0],
                fontDefinitionProvider = params[1],
                bibleVersionRepository = get(),
                languagesRepository = LanguageRepository(),
                store = get(),
            )
        }
    }
