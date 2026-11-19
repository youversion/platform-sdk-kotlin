package com.youversion.platform.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youversion.platform.ui.components.GroupedCard
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.icons.BibleCardIcon
import com.youversion.platform.ui.icons.NavigateIcon
import com.youversion.platform.ui.icons.VerseOfTheDayIcon

/**
 * The Examples tab: a list of SDK examples, each opening its own screen.
 *
 * @param onDestinationClick Called when the user picks a tab in the bottom bar.
 * @param onExampleClick Called with the example's destination when the user picks an example.
 */
@Composable
fun ExamplesViewTab(
    onDestinationClick: (SampleDestination) -> Unit,
    onExampleClick: (SampleDestination) -> Unit,
) {
    Scaffold(
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
                    .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Examples",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
            GroupedCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                ExampleRow(
                    title = "Navigate",
                    icon = NavigateIcon,
                    onClick = { onExampleClick(SampleDestination.Navigate) },
                )
                ExampleDivider()
                ExampleRow(
                    title = "Verse of the Day",
                    icon = VerseOfTheDayIcon,
                    onClick = { onExampleClick(SampleDestination.Votd) },
                )
                ExampleDivider()
                ExampleRow(
                    title = "Card",
                    icon = BibleCardIcon,
                    onClick = { onExampleClick(SampleDestination.BibleCard) },
                )
            }
        }
    }
}

@Composable
private fun ExampleRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun ExampleDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
    )
}
