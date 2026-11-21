package com.youversion.platform.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.core.verseimages.models.VerseImage
import com.youversion.platform.core.videos.models.VideoPlaylist
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.components.SegmentedSwitch
import com.youversion.platform.ui.content.ContentCollections
import com.youversion.platform.ui.content.ContentCollectionsState
import com.youversion.platform.ui.content.ContentImagesViewModel
import com.youversion.platform.ui.content.ContentLanguagePicker
import com.youversion.platform.ui.content.ContentVideosState
import com.youversion.platform.ui.content.VerseImageView
import com.youversion.platform.ui.content.contentLanguageName
import com.youversion.platform.ui.content.passageTitle
import com.youversion.platform.ui.icons.BookClosedIcon

/**
 * The Content tab: BibleProject's video collections, or a searchable, paged grid of verse images, in one content
 * language.
 *
 * @param languageTag The content language.
 * @param onLanguageSelect Called with the content language the user picks.
 * @param collectionsState The collection list.
 * @param videosState The videos loaded so far.
 * @param onRetryCollections Called when the user retries loading the collections.
 * @param onLoadVideos Called with video ids whose details should load.
 * @param viewModel The image library's state.
 * @param onDestinationClick Called when the user picks a tab in the bottom bar.
 * @param onCollectionClick Called with the collection the user taps.
 * @param onImageClick Called with the image the user taps.
 */
@Composable
internal fun ContentViewTab(
    languageTag: String,
    onLanguageSelect: (String) -> Unit,
    collectionsState: ContentCollectionsState,
    videosState: ContentVideosState,
    onRetryCollections: () -> Unit,
    onLoadVideos: (List<String>) -> Unit,
    viewModel: ContentImagesViewModel,
    onDestinationClick: (SampleDestination) -> Unit,
    onCollectionClick: (VideoPlaylist) -> Unit,
    onImageClick: (VerseImage) -> Unit,
) {
    var showsImages by rememberSaveable { mutableStateOf(false) }
    var showsLanguages by rememberSaveable { mutableStateOf(false) }
    val collectionsListState = rememberLazyListState()
    val imagesGridState = rememberLazyGridState()

    Scaffold(
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Content,
                onDestinationClick = onDestinationClick,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Content",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showsLanguages = true }) {
                        Icon(
                            Icons.Filled.Language,
                            contentDescription = "Content language, ${contentLanguageName(languageTag)}",
                        )
                    }
                }
                SegmentedSwitch(
                    labels = listOf("Videos", "Images"),
                    selectedIndex = if (showsImages) 1 else 0,
                    onSelect = { showsImages = it == 1 },
                    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                    selectedContentColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (showsImages) {
                ContentImages(viewModel = viewModel, gridState = imagesGridState, onImageClick = onImageClick)
            } else {
                ContentCollections(
                    state = collectionsState,
                    videosState = videosState,
                    listState = collectionsListState,
                    onRetry = onRetryCollections,
                    onLoadVideos = onLoadVideos,
                    onCollectionClick = onCollectionClick,
                )
            }
        }
    }

    if (showsLanguages) {
        ContentLanguagePicker(
            selectedLanguageTag = languageTag,
            onSelect = onLanguageSelect,
            onDismiss = { showsLanguages = false },
        )
    }
}

@Composable
private fun ContentImages(
    viewModel: ContentImagesViewModel,
    gridState: LazyGridState,
    onImageClick: (VerseImage) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(state.query) }
    val focusManager = LocalFocusManager.current

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search by verse reference") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    query = ""
                                    viewModel.search("")
                                },
                            ) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    isError = state.isQueryTooLong,
                    supportingText =
                        if (state.isQueryTooLong) {
                            { Text("Enter a search of 100 characters or fewer.") }
                        } else {
                            null
                        },
                    singleLine = true,
                    shape = CircleShape,
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                        ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions =
                        KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                viewModel.search(query)
                            },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.hasVersionError) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Passage names didn’t load.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::retry) { Text("Retry") }
                    }
                }
            }
        }

        items(state.images, key = { it.id }) { image ->
            val title = passageTitle(state.bibleVersion, image.passageId)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                ) {
                    VerseImageView(
                        image = image,
                        contentDescription = title,
                        onClick = { onImageClick(image) },
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        BookClosedIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator()
                    }

                    state.hasError -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "The images didn’t load.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = viewModel::retry) { Text("Retry") }
                        }
                    }

                    state.hasMorePages -> {
                        TextButton(onClick = viewModel::loadMore) { Text("Load more") }
                    }

                    state.images.isEmpty() && !state.isQueryTooLong -> {
                        Text(
                            text =
                                if (state.query.isEmpty()) {
                                    "No images found"
                                } else {
                                    "There are no images for ${state.query}"
                                },
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
