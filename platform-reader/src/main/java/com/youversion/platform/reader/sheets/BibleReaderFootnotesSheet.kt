package com.youversion.platform.reader.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.ui.theme.UntitledSerif
import com.youversion.platform.ui.views.BibleText
import com.youversion.platform.ui.views.BibleTextFonts
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import com.youversion.platform.ui.views.BibleTextOptions
import com.youversion.platform.ui.views.convertToEnumeration
import com.youversion.platform.ui.views.rendering.BibleReferenceAttribute
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import kotlinx.coroutines.CancellationException

// The sheet ignores the user's reader font settings and renders everything at a fixed size.
private val SheetTextOptions =
    BibleTextOptions(
        fontFamily = UntitledSerif,
        fontSize = 16.sp,
        renderHeadlines = false,
        renderVerseNumbers = false,
        footnoteMode = BibleTextFootnoteMode.LETTERS,
        footnoteMarker = null,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderFootnotesSheet(
    onDismissRequest: () -> Unit,
    version: BibleVersion?,
    reference: BibleReference?,
    footnotes: List<AnnotatedString>,
) {
    val sheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // The passed-in footnotes were rendered with the user's Bible font family and size, which might look odd in
    // this sheet; re-render them at a fixed size, keeping the passed-in list as a fallback.
    var displayFootnotes by remember { mutableStateOf(footnotes) }

    LaunchedEffect(reference) {
        val ref = reference ?: return@LaunchedEffect
        val chapterRepository: BibleChapterRepository = PlatformKoinGraph.koinApplication.koin.get()
        val blocks =
            try {
                BibleVersionRendering.textBlocks(
                    bibleChapterRepository = chapterRepository,
                    reference = ref,
                    renderVerseNumbers = false,
                    renderHeadlines = false,
                    footnoteMode = BibleTextFootnoteMode.LETTERS,
                    footnoteMarker = null,
                    textColor = Color.Unspecified,
                    wocColor = Color.Unspecified,
                    fonts =
                        BibleTextFonts(
                            fontFamily = SheetTextOptions.fontFamily,
                            baseSize = SheetTextOptions.fontSize,
                        ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }

        if (blocks != null) {
            val referenceAnnotation = "${ref.versionId}:${ref.bookUSFM}:${ref.chapter}:${ref.verseStart}"
            val renderedFootnotes =
                blocks
                    .flatMap { it.footnotes }
                    .filter { footnote ->
                        footnote
                            .getStringAnnotations(BibleReferenceAttribute.NAME, 0, footnote.length)
                            .any { it.item == referenceAnnotation }
                    }
            if (renderedFootnotes.isNotEmpty()) {
                displayFootnotes = renderedFootnotes
            }
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(
                                color = Color.Black,
                                shape = RoundedCornerShape(2.dp),
                            ),
                )
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .padding(vertical = 16.dp)
                    .height(360.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
                        textOptions = SheetTextOptions,
                    )
                }
                Footnotes(footnotes = displayFootnotes)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun Footnotes(footnotes: List<AnnotatedString>) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).testTag("footnote_divider"))
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
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).testTag("footnote_divider"))
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
                    versionId = BibleDefaults.VERSION_ID,
                    bookUSFM = "2CO",
                    chapter = 1,
                    verseStart = 3,
                    verseEnd = 5,
                ),
            footnotes = listOf(AnnotatedString("1:5 Footnote details")),
        )
    }
}
