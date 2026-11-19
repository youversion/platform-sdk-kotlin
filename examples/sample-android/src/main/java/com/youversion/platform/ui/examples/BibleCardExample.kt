package com.youversion.platform.ui.examples

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.views.card.BibleCard

/**
 * Shows a Bible card for a fixed passage, with its version picker.
 *
 * @param onDestinationClick Called when the user picks a tab in the bottom bar.
 * @param onBack Called when the user taps the back arrow.
 */
@Composable
fun BibleCardExample(
    onDestinationClick: (SampleDestination) -> Unit,
    onBack: () -> Unit,
) {
    val bibleReference =
        BibleReference(
            versionId = BibleDefaults.VERSION_ID,
            bookUSFM = "2CO",
            chapter = 1,
            verseStart = 3,
            verseEnd = 4,
        )

    Scaffold(
        topBar = {
            ExampleTopBar(
                title = "Card",
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
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            BibleCard(
                reference = bibleReference,
                fontSize = 18.sp,
                showVersionPicker = true,
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun Preview_BibleCardExample() {
    MaterialTheme {
        Surface {
            BibleCardExample(onDestinationClick = {}, onBack = {})
        }
    }
}
