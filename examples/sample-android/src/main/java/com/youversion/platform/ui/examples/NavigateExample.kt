package com.youversion.platform.ui.examples

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.ui.components.GroupedCard
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination

private val passages =
    listOf(
        "John 3:16" to BibleReference(BibleDefaults.VERSION_ID, "JHN", 3, 16),
        "Psalm 119:105" to BibleReference(BibleDefaults.VERSION_ID, "PSA", 119, 105),
        "Romans 8:28–39" to BibleReference(BibleDefaults.VERSION_ID, "ROM", 8, 28, 39),
        "Genesis 1" to BibleReference(BibleDefaults.VERSION_ID, "GEN", 1),
    )

@Composable
fun NavigateExample(
    onDestinationClick: (SampleDestination) -> Unit,
    onBack: () -> Unit,
    onReadInBible: (BibleReference) -> Unit,
) {
    Scaffold(
        topBar = {
            ExampleTopBar(
                title = "Navigate",
                onBack = onBack,
            )
        },
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Examples,
                onDestinationClick = onDestinationClick,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(top = 8.dp)) {
            GroupedCard(containerColor = MaterialTheme.colorScheme.surface) {
                passages.forEachIndexed { index, (title, reference) ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    ListItem(
                        headlineContent = { Text(title, color = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onReadInBible(reference) },
                    )
                }
            }
            Text(
                text = "Tapping a passage opens it in the Bible tab.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            )
        }
    }
}
