package com.youversion.platform.ui.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.verseimages.models.VerseImage
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.content.VerseImageStack
import com.youversion.platform.ui.content.passageTitle
import com.youversion.platform.ui.views.votd.CompactVerseOfTheDay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

/**
 * Shows the Verse of the Day, either as the compact component or as the day's verse images.
 *
 * @param onDestinationClick Called when the user picks a tab in the bottom bar.
 * @param onBack Called when the user taps the back arrow.
 */
@Composable
fun VotdExample(
    onDestinationClick: (SampleDestination) -> Unit,
    onBack: () -> Unit,
) {
    var showsImages by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ExampleTopBar(
                title = "Verse of the Day",
                onBack = onBack,
            )
        },
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Examples,
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
                    .padding(24.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                        .padding(2.dp)
                        .selectableGroup(),
            ) {
                listOf("VotdView", "Verse Images").forEachIndexed { index, label ->
                    val isSelected = showsImages == (index == 1)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .weight(1f)
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .shadow(2.dp, CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceBright, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                ).clip(CircleShape)
                                .selectable(
                                    selected = isSelected,
                                    role = Role.Tab,
                                    onClick = { showsImages = index == 1 },
                                ).heightIn(min = 32.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
            if (showsImages) {
                VotdImages()
            } else {
                CompactVerseOfTheDay()
            }
        }
    }
}

/** The midnight, in UTC milliseconds, of today's local date, which is how [DatePicker] represents a day. */
private fun todayUtcMillis(): Long {
    val local = Calendar.getInstance()
    return Calendar
        .getInstance(UTC)
        .apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
}

@Composable
private fun VotdImages() {
    val todayMillis = remember { todayUtcMillis() }
    var selectedDateMillis by rememberSaveable { mutableLongStateOf(todayMillis) }
    var retryCount by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf<String?>(null) }
    var images by remember { mutableStateOf<List<VerseImage>?>(null) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDateMillis, retryCount) {
        title = null
        images = null
        hasError = false
        try {
            val dayOfYear =
                Calendar
                    .getInstance(
                        UTC,
                    ).apply { timeInMillis = selectedDateMillis }
                    .get(Calendar.DAY_OF_YEAR)
            coroutineScope {
                val verse = async { YouVersionApi.votd.verseOfTheDay(dayOfYear) }
                val version = async { YouVersionApi.bible.version(BibleDefaults.VERSION_ID) }
                val passageId = verse.await().passageUsfm
                val page =
                    YouVersionApi.verseImages.verseImages(
                        passageId = passageId,
                        languageRanges = listOf(Locale.getDefault().language),
                    )
                title = passageTitle(version.await(), passageId)
                images = page.data
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            hasError = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "VERSE OF THE DAY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title ?: "Verse of the Day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        VotdDatePicker(
            selectedDateMillis = selectedDateMillis,
            todayMillis = todayMillis,
            onDateChange = { selectedDateMillis = it },
        )

        val loadedImages = images
        when {
            loadedImages != null && loadedImages.isNotEmpty() -> {
                key(selectedDateMillis) {
                    VerseImageStack(
                        images = loadedImages,
                        titleOf = { title ?: it.passageId },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            loadedImages != null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                ) {
                    Text(
                        text = "(No images are available)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            hasError -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.WifiOff, contentDescription = null)
                    Text("Couldn’t load images", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Check your connection and try again.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { retryCount++ }) { Text("Try again") }
                }
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Loading images…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VotdDatePicker(
    selectedDateMillis: Long,
    todayMillis: Long,
    onDateChange: (Long) -> Unit,
) {
    var showsDialog by remember { mutableStateOf(false) }
    val dateText =
        remember(selectedDateMillis) {
            DateFormat
                .getDateInstance(DateFormat.MEDIUM)
                .apply { timeZone = UTC }
                .format(selectedDateMillis)
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
    ) {
        IconButton(onClick = { onDateChange(selectedDateMillis - DAY_MILLIS) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        TextButton(onClick = { showsDialog = true }, modifier = Modifier.weight(1f)) {
            Text(dateText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(
            onClick = { onDateChange(selectedDateMillis + DAY_MILLIS) },
            enabled = selectedDateMillis < todayMillis,
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
    }

    if (showsDialog) {
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = selectedDateMillis,
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
                    },
            )
        DatePickerDialog(
            onDismissRequest = { showsDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let(onDateChange)
                        showsDialog = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showsDialog = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
