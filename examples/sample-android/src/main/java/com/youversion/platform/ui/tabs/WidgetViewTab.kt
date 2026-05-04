package com.youversion.platform.ui.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

@Composable
fun WidgetViewTab(onDestinationClick: (SampleDestination) -> Unit) {
    val bibleReference =
        BibleReference(
            versionId = BibleDefaults.VERSION_ID,
            bookUSFM = "2CO",
            chapter = 1,
            verseStart = 3,
            verseEnd = 20,
        )

    Scaffold(
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Widget,
                onDestinationClick = onDestinationClick,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                BibleCard(
                    reference = bibleReference,
                    fontSize = 16.sp,
                    showVersionPicker = true,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_WidgetViewTab() {
    MaterialTheme {
        Surface {
            WidgetViewTab(onDestinationClick = {})
        }
    }
}
