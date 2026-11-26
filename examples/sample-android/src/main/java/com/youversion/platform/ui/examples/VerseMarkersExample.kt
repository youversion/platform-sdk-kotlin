package com.youversion.platform.ui.examples

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.views.BibleText
import com.youversion.platform.ui.views.BibleTextOptions
import com.youversion.platform.ui.views.BibleVerseIndicatorStyle
import kotlinx.coroutines.launch

/**
 * Shows the verse indicator hooks on [BibleText] with a generic marker: tapping a verse marks its number, and
 * tapping a marker reports its verse.
 *
 * @param onDestinationClick Called when the user picks a tab in the bottom bar.
 * @param onBack Called when the user taps the back arrow.
 */
@Composable
fun VerseMarkersExample(
    onDestinationClick: (SampleDestination) -> Unit,
    onBack: () -> Unit,
) {
    val chapter = remember { BibleReference(versionId = BibleDefaults.VERSION_ID, bookUSFM = "JHN", chapter = 1) }
    var markedVerses by remember { mutableStateOf(setOf<BibleReference>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val markerStyle = BibleVerseIndicatorStyle(backgroundColor = Color(0xFF2B6BE6), foregroundColor = Color.White)

    Scaffold(
        topBar = { ExampleTopBar(title = "Verse Markers", onBack = onBack) },
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Examples,
                onDestinationClick = onDestinationClick,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            Text(
                text = "Tap a verse to mark its number. Tap a marker to see which verse it belongs to.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            BibleText(
                reference = chapter,
                textOptions = BibleTextOptions(fontSize = 18.sp),
                onVerseTap = { reference, _ ->
                    markedVerses = if (reference in markedVerses) markedVerses - reference else markedVerses + reference
                },
                verseIndicatorStyle = { reference -> markerStyle.takeIf { reference in markedVerses } },
                onVerseIndicatorTap = { reference ->
                    scope.launch { snackbarHostState.showSnackbar("Marker on ${reference.asUSFM}") }
                },
            )
        }
    }
}
