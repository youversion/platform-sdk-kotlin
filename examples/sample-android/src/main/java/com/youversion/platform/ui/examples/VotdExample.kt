package com.youversion.platform.ui.examples

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.views.votd.CompactVerseOfTheDay

/**
 * Shows the compact Verse of the Day component.
 *
 * @param onDestinationClick Called when the user picks a tab in the bottom bar.
 * @param onBack Called when the user taps the back arrow.
 */
@Composable
fun VotdExample(
    onDestinationClick: (SampleDestination) -> Unit,
    onBack: () -> Unit,
) {
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
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
        ) {
            CompactVerseOfTheDay()
        }
    }
}
