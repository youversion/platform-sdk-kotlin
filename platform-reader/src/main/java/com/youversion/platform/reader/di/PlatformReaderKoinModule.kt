package com.youversion.platform.reader.di

import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.CopyManager
import com.youversion.platform.reader.domain.ShareManager
import com.youversion.platform.reader.domain.UserSettingsRepository
import com.youversion.platform.reader.screens.languages.LanguagesViewModel
import com.youversion.platform.reader.screens.versions.VersionsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val PlatformReaderKoinModule =
    module {
        // Repositories
        singleOf(::BibleReaderRepository)
        factoryOf(::UserSettingsRepository)
        factoryOf(::CopyManager)
        factoryOf(::ShareManager)

        // View Models
        factory { params ->
            BibleReaderViewModel(
                bibleReference = params[0],
                fontDefinitionProvider = params[1],
                bibleVersionRepository = get(),
                bibleReaderRepository = get(),
                userSettingsRepository = get(),
                bibleChapterRepository = get(),
                languageRepository = get(),
                copyManager = get(),
                shareManager = get(),
            )
        }

        factoryOf(::VersionsViewModel)

        factory { params ->
            LanguagesViewModel(
                bibleVersion = params[0],
                languageRepository = get(),
            )
        }
    }
