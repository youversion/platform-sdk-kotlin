package com.youversion.platform.reader.screens.languages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.components.BibleReaderTopAppBar
import com.youversion.platform.reader.theme.readerColorScheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class LanguageTab(
    val label: String,
) {
    SUGGESTED("Suggested"),
    ALL("All"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguagesScreen(
    bibleVersion: BibleVersion?,
    onBackClick: () -> Unit,
) {
    val viewModel: LanguagesViewModel = koinViewModel { parametersOf(bibleVersion) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val pagerState =
        rememberPagerState(initialPage = LanguageTab.SUGGESTED.ordinal) { 2 }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            BibleReaderTopAppBar(
                title = "Select a Languages",
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = BibleReaderTheme.colorScheme.canvasPrimary,
                contentColor = BibleReaderTheme.colorScheme.textPrimary,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(pagerState.targetPage, matchContentSize = true),
                        color = BibleReaderTheme.colorScheme.textPrimary,
                        width = Dp.Unspecified,
                    )
                },
            ) {
                LanguageTab.entries.forEachIndexed { index, destination ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(text = destination.label)
                        },
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
            ) { page ->
                when (page) {
                    0 -> {
                        LanguagesTab(
                            languages = state.suggestedLanguages,
                            showProgress = state.initializing,
                            onLanguageClick = { /* TODO */ },
                        )
                    }

                    1 -> {
                        LanguagesTab(
                            languages = state.allLanguages,
                            showProgress = state.initializing,
                            onLanguageClick = { /* TODO */ },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagesTab(
    languages: List<LanguageRowItem>,
    showProgress: Boolean,
    onLanguageClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (showProgress) {
            item {
                CircularProgressIndicator()
            }
        }
        items(
            items = languages,
        ) { language ->
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = null,
                            enabled = true,
                            indication = ripple(),
                            onClick = {
                                onLanguageClick(language.languageTag)
                            },
                        ).padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = language.displayName,
                    color = MaterialTheme.readerColorScheme.readerTextPrimaryColor,
                )
                language.localeDisplayName?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.readerColorScheme.readerTextMutedColor,
                    )
                }
            }
        }
    }
}
