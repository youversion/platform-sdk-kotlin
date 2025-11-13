package com.youversion.platform.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.views.votd.CompactVerseOfTheDay
import com.youversion.platform.ui.views.votd.VerseOfTheDay

@Composable
fun VotdViewTab(onDestinationClick: (SampleDestination) -> Unit) {
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Votd,
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
                    .padding(innerPadding)
                    .padding(20.dp),
        ) {
            Column {
                CompactVerseOfTheDay()

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                VerseOfTheDay(
                    onShareClick = { Toast.makeText(context, "Share clicked", Toast.LENGTH_SHORT).show() },
                    onFullChapterClick = { Toast.makeText(context, "Full Chapter clicked", Toast.LENGTH_SHORT).show() },
                )
            }
        }
    }
}
