package com.youversion.platform.reader.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.ui.views.BibleText
import com.youversion.platform.ui.views.BibleTextOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderFootnotesSheet(
    onDismissRequest: () -> Unit,
    reference: BibleReference?,
    footnotes: List<AnnotatedString>,
) {
    val sheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier =
                Modifier
                    .padding(vertical = 16.dp)
                    .height(240.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier =
                    Modifier
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                reference?.let {
                    BibleText(
                        reference = it,
                        textOptions =
                            BibleTextOptions(
                                renderHeadlines = false,
                            ),
                    )
                }
                Footnotes(footnotes = footnotes)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun Footnotes(footnotes: List<AnnotatedString>) {
    Column {
        HorizontalDivider()
        footnotes.forEach {
            Text(it)
            HorizontalDivider()
        }
    }
}

@Preview
@Composable
private fun Preview_BibleReaderFootnotesSheet() {
    MaterialTheme {
        BibleReaderFootnotesSheet(
            onDismissRequest = {},
            reference =
                BibleReference(
                    versionId = 111,
                    bookUSFM = "2CO",
                    chapter = 1,
                    verseStart = 3,
                    verseEnd = 5,
                ),
            footnotes = listOf(AnnotatedString("1:5 Footnote details")),
        )
    }
}
