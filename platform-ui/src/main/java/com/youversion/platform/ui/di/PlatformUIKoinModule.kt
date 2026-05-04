package com.youversion.platform.ui.di

import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.ui.views.card.BibleCardViewModel
import com.youversion.platform.ui.views.versions.BibleVersionsViewModel
import com.youversion.platform.ui.views.votd.VerseOfTheDayViewModel
import org.koin.dsl.module

val PlatformUIKoinModule =
    module {

        // View Models
        factory { params ->
            BibleCardViewModel(
                reference = params[0],
                bibleVersion = params[1],
                bibleVersionRepository = get(),
            )
        }

        factory { params ->
            VerseOfTheDayViewModel(
                bibleVersionId = params[0],
                bibleVersionRepository = get(),
            )
        }

        factory { params ->
            BibleVersionsViewModel(
                initialVersionId = params.getOrNull<Int>(),
                onVersionChange = params.getOrNull<(BibleVersion) -> Unit>() ?: {},
                languageRepository = get(),
                bibleVersionRepository = get(),
            )
        }
    }
