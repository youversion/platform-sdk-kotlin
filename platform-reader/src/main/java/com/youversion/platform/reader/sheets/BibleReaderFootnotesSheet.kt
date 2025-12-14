package com.youversion.platform.reader.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.ui.views.BibleText
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import com.youversion.platform.ui.views.BibleTextOptions
import com.youversion.platform.ui.views.convertToEnumeration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderFootnotesSheet(
    textOptions: BibleTextOptions,
    onDismissRequest: () -> Unit,
    version: BibleVersion?,
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
                    .height(360.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier =
                    Modifier
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                if (version != null && reference != null) {
                    Text(
                        text = version.displayTitle(reference, includesVersionAbbreviation = true),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                reference?.let {
                    BibleText(
                        reference = it,
                        textOptions =
                            textOptions.copy(
                                renderHeadlines = false,
                                renderVerseNumbers = false,
                                footnoteMode = BibleTextFootnoteMode.LETTERS,
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
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        footnotes.forEachIndexed { index, footnote ->
            val enumeration = index.convertToEnumeration() + "."
            val style = footnote.spanStyles.firstOrNull()?.item

            val footnoteEnumeration =
                buildAnnotatedString {
                    style?.run {
                        withStyle(this) {
                            append(enumeration)
                        }
                    } ?: append(enumeration)
                }
            Row {
                Text(footnoteEnumeration, modifier = Modifier.padding(end = 4.dp))
                Text(footnote)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Preview
@Composable
private fun Preview_BibleReaderFootnotesSheet() {
    MaterialTheme {
        BibleReaderFootnotesSheet(
            onDismissRequest = {},
            version = BibleVersion.preview,
            reference =
                BibleReference(
                    versionId = 111,
                    bookUSFM = "2CO",
                    chapter = 1,
                    verseStart = 3,
                    verseEnd = 5,
                ),
            textOptions = BibleTextOptions(),
            footnotes = listOf(AnnotatedString("1:5 Footnote details")),
        )
    }
}
