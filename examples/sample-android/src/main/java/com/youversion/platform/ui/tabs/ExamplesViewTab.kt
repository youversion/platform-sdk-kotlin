package com.youversion.platform.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youversion.platform.ui.components.GroupedCard
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.icons.BibleCardIcon
import com.youversion.platform.ui.icons.NavigateIcon
import com.youversion.platform.ui.icons.VerseOfTheDayIcon

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
        Column(modifier = Modifier.padding(innerPadding)) {
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
                    title = "Bible Card",
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
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ExampleDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
    )
}
