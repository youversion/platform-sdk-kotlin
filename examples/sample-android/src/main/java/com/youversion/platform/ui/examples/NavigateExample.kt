package com.youversion.platform.ui.examples

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.ui.components.GroupedCard
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.theme.LinkColor

private val passages =
    listOf(
        "John 3:16" to BibleReference(BibleDefaults.VERSION_ID, "JHN", 3, 16),
        "Psalm 119:105" to BibleReference(BibleDefaults.VERSION_ID, "PSA", 119, 105),
        "Romans 8:28–39" to BibleReference(BibleDefaults.VERSION_ID, "ROM", 8, 28, 39),
        "Genesis 1" to BibleReference(BibleDefaults.VERSION_ID, "GEN", 1),
    )

/**
 * Lists passages that open in the Bible tab, showing how a host app sends the reader to a reference.
 *
 * @param onDestinationClick Called when the user picks a tab in the bottom bar.
 * @param onBack Called when the user taps the back arrow.
 * @param onReadInBible Called with the passage the user picks, to open it in the Bible tab.
 */
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
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp),
        ) {
            GroupedCard(containerColor = MaterialTheme.colorScheme.surface) {
                passages.forEachIndexed { index, (title, reference) ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onReadInBible(reference) }
                                .padding(horizontal = 16.dp),
                    ) {
                        Text(title, color = LinkColor, style = MaterialTheme.typography.bodyLarge)
                    }
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
