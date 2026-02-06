package com.youversion.platform.reader.sheets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.bibles.domain.BibleReference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderVerseActionSheet(
    selectedVerses: Set<BibleReference>,
    onDismiss: () -> Unit,
) {
    val sheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {}

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
