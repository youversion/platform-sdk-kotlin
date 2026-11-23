package com.youversion.platform.ui.content

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.verseimages.models.VerseImage
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.icons.BookClosedIcon
import com.youversion.platform.ui.theme.LinkColor
import com.youversion.platform.ui.theme.linkTextColor
import com.youversion.platform.ui.views.card.BibleCard
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val PASSAGE_PAGE_SIZE = 99

private val verseImagesSerializer = ListSerializer(VerseImage.serializer())

private val verseImagesSaver =
    Saver<List<VerseImage>, String>(
        save = { Json.encodeToString(verseImagesSerializer, it) },
        restore = { Json.decodeFromString(verseImagesSerializer, it) },
    )

/**
 * Shows [image] on top of a stack of the other art for its passage. Tapping the top card opens it full screen,
 * and the share button shares whichever card is on top.
 *
 * @param image The image the user opened.
 * @param bibleVersion The version used to title the passage and read it, or `null` until it loads.
 * @param onBack Called when the user taps the back arrow.
 * @param onDestinationClick Called when the user picks a tab in the bottom bar.
 * @param onReadInBible Called with the passage to open in the Bible tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContentImageDetail(
    image: VerseImage,
    bibleVersion: BibleVersion?,
    onBack: () -> Unit,
    onDestinationClick: (SampleDestination) -> Unit,
    onReadInBible: (BibleReference) -> Unit,
) {
    val context = LocalContext.current
    var images by rememberSaveable(image.id, stateSaver = verseImagesSaver) { mutableStateOf(listOf(image)) }
    var hasError by remember(image.id) { mutableStateOf(false) }
    var retryCount by remember(image.id) { mutableIntStateOf(0) }
    var selectedImageId by rememberSaveable(image.id) { mutableStateOf(image.id) }
    val displayedImage = images.firstOrNull { it.id == selectedImageId } ?: images.first()
    var isViewerOpen by rememberSaveable(image.id) { mutableStateOf(false) }

    LaunchedEffect(image.id, retryCount) {
        hasError = false
        try {
            var pageToken: String? = null
            val visitedTokens = mutableSetOf<String>()
            do {
                val page =
                    YouVersionApi.verseImages.verseImages(
                        passageId = image.passageId,
                        languageRanges = listOf(CONTENT_LANGUAGE_TAG),
                        pageSize = PASSAGE_PAGE_SIZE,
                        pageToken = pageToken,
                    )
                val loadedIds = images.mapTo(mutableSetOf()) { it.id }
                images = images + page.data.filter { loadedIds.add(it.id) }
                pageToken = page.nextPageToken?.takeIf { visitedTokens.add(it) }
            } while (pageToken != null)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            hasError = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            shareImageLink(
                                context,
                                displayedImage,
                                passageTitle(bibleVersion, displayedImage.passageId),
                            )
                        },
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share link")
                    }
                },
            )
        },
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Content,
                onDestinationClick = onDestinationClick,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Verse Images",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = passageTitle(bibleVersion, image.passageId),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            VerseImageStack(
                images = images,
                selectedImageId = selectedImageId,
                onSelectedImageIdChange = { selectedImageId = it },
                titleOf = { passageTitle(bibleVersion, it.passageId) },
                onImageClick = { isViewerOpen = true },
                modifier = Modifier.fillMaxWidth(),
            )
            if (hasError) {
                Text(
                    text = "More images for this passage didn’t load.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { retryCount++ }) { Text("Retry") }
            }
        }
    }

    if (isViewerOpen) {
        ContentImageViewer(
            image = displayedImage,
            bibleVersion = bibleVersion,
            onClose = { isViewerOpen = false },
            onReadInBible = { reference ->
                isViewerOpen = false
                onReadInBible(reference)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentImageViewer(
    image: VerseImage,
    bibleVersion: BibleVersion?,
    onClose: () -> Unit,
    onReadInBible: (BibleReference) -> Unit,
) {
    val context = LocalContext.current
    val title = passageTitle(bibleVersion, image.passageId)
    val reference =
        bibleVersion?.reference(image.passageId)
            ?: BibleReference.unvalidatedReference(image.passageId, BibleDefaults.VERSION_ID)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Verse image", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, contentDescription = "Close image")
                        }
                    },
                    actions = {
                        IconButton(onClick = { shareImageLink(context, image, title) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share link")
                        }
                    },
                )
            },
            bottomBar = {
                Button(
                    onClick = { reference?.let(onReadInBible) },
                    enabled = reference != null,
                    colors = ButtonDefaults.buttonColors(containerColor = LinkColor, contentColor = Color.White),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                ) {
                    Icon(BookClosedIcon, contentDescription = null)
                    Text("Read in Bible", modifier = Modifier.padding(start = 8.dp))
                }
            },
            contentWindowInsets = WindowInsets(0),
            modifier = Modifier.fillMaxHeight(),
        ) { innerPadding ->
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
            ) {
                VerseImageView(
                    image = image,
                    contentDescription = title,
                    size = VerseImageSize.Full,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Verses in this image", style = MaterialTheme.typography.labelMedium)
                FilledTonalButton(
                    onClick = { reference?.let(onReadInBible) },
                    enabled = reference != null,
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = linkTextColor,
                        ),
                    contentPadding = PaddingValues(start = 24.dp, end = 16.dp),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    }
                }
                if (reference != null) {
                    BibleCard(reference = reference, version = bibleVersion, fontSize = 18.sp)
                }
                Text(
                    text = image.attribution,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun shareImageLink(
    context: Context,
    image: VerseImage,
    title: String,
) {
    val intent =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .putExtra(Intent.EXTRA_TEXT, "$title\n${image.imageUrl}")
    context.startActivity(Intent.createChooser(intent, null))
}
