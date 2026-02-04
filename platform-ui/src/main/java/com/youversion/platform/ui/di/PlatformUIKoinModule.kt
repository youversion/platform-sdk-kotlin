package com.youversion.platform.ui.di

import com.youversion.platform.ui.views.widget.BibleWidgetViewModel
import org.koin.dsl.module

internal val PlatformUIKoinModule =
    module {

        // View Models
        factory { params ->
            BibleWidgetViewModel(
                reference = params[0],
                bibleVersion = params[1],
                bibleVersionRepository = get(),
            )
        }
    }
