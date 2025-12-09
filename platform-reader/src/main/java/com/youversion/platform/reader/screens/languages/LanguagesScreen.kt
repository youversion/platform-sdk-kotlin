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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.core.languages.models.Language
import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.components.BibleReaderTopAppBar
import com.youversion.platform.reader.theme.readerColorScheme
import kotlinx.coroutines.launch

private enum class LanguageTab(
    val label: String,
) {
    SUGGESTED("Suggested"),
    ALL("All"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguagesScreen(
    viewModel: BibleReaderViewModel,
    onBackClick: () -> Unit,
) {
//    val viewModel: LanguagesViewModel =
//        viewModel(factory = LanguagesViewModel.factory(permittedVersions = permittedVersions))
    val state by viewModel.state.collectAsStateWithLifecycle()

    val startDestination = LanguageTab.SUGGESTED
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            BibleReaderTopAppBar(
                title = "Languages",
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PrimaryTabRow(
                selectedTabIndex = selectedDestination,
            ) {
                LanguageTab.entries.forEachIndexed { index, destination ->
                    Tab(
                        selected = selectedDestination == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                            selectedDestination = index
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
                    0 ->
                        SuggestedLanguagesTab(
                            languages = state.suggestedLanguages,
                            onLanguageClick = { /* TODO */ },
                        )
                    1 -> AllLanguagesTab()
                }
            }
        }
    }
}

@Composable
private fun SuggestedLanguagesTab(
    languages: List<LanguageRowItem>,
    onLanguageClick: (Language) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
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
                            onClick = { onLanguageClick(language.language) },
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

@Composable
private fun AllLanguagesTab() {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        Text("All Languages")
        Text("All Languages")
        Text("All Languages")
        Text("All Languages")
        Text("All Languages")
        Text("All Languages")
        Text("All Languages")
        Text("All Languages")
        Text("All Languages")
        Text("All Languages")
    }
}
