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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.reader.R
import com.youversion.platform.ui.theme.UntitledSerif

// Matches the fixed size the chapter footnotes sheet renders at, so both sheets ignore the reader's font settings.
private val SheetFootnoteStyle =
    SpanStyle(
        fontFamily = UntitledSerif,
        fontSize = 16.sp,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BibleReaderIntroFootnotesSheet(
    onDismissRequest: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(360.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.footnote_header_label),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Column {
                footnotes.forEach { footnote ->
                    Row {
                        Text(footnote.atSheetSize())
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * A copy of this footnote pinned to the sheet's font family and size.
 *
 * The footnote was rendered with the reader's font settings. Span styles merge attribute by attribute with the
 * last one winning, so overlaying [SheetFootnoteStyle] replaces only the family and size, leaving emphasis and
 * color intact.
 */
private fun AnnotatedString.atSheetSize(): AnnotatedString =
    buildAnnotatedString {
        append(this@atSheetSize)
        addStyle(SheetFootnoteStyle, 0, length)
    }
