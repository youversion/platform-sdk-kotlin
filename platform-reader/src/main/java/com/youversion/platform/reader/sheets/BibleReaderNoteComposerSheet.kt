package com.youversion.platform.reader.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.reader.domain.NoteDraft
import com.youversion.platform.reader.icons.BookmarkIcon
import com.youversion.platform.ui.R as UiR

/**
 * The notes composer for [draft]: its passage, a text field, a Private/Public control, the highlight colors in palette
 * order, and Cancel and Save.
 *
 * Swiping the sheet away or tapping Cancel calls [onDismissRequest] rather than closing it, so the reader is asked
 * before unsaved changes are lost; the sheet closes once the draft is gone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BibleReaderNoteComposerSheet(
    draft: NoteDraft,
    onUpdate: (content: String, isPublic: Boolean, hexColor: String?) -> Unit,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onCancelDiscard: () -> Unit,
    onDismissSaveError: () -> Unit,
) {
    val currentDraft by rememberUpdatedState(draft)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { value ->
                if (value == SheetValue.Hidden && (currentDraft.hasChanges || currentDraft.isSaving)) {
                    currentOnDismissRequest()
                    false
                } else {
                    true
                }
            },
        )
    LaunchedEffect(draft.showsDiscardConfirmation) {
        if (!draft.showsDiscardConfirmation && !sheetState.isVisible) {
            sheetState.show()
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("note_composer_sheet"),
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NoteHeader(
                referenceTitle = draft.referenceTitle,
                isPublic = draft.isPublic,
                isEnabled = !draft.isSaving,
                onToggleVisibility = { onUpdate(draft.content, !draft.isPublic, draft.hexColor) },
            )
            NoteTextField(
                content = draft.content,
                isEnabled = !draft.isSaving,
                onContentChange = { onUpdate(it, draft.isPublic, draft.hexColor) },
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).alpha(if (draft.isSaving) 0.5f else 1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HighlightColor.entries.forEach { highlightColor ->
                    val isSelected = draft.hexColor == highlightColor.hexColor
                    HighlightColorButton(
                        highlightColor = highlightColor,
                        isSelected = isSelected,
                        contentDescription = stringResource(highlightColor.nameResId),
                        onClick = {
                            if (!draft.isSaving) {
                                onUpdate(
                                    draft.content,
                                    draft.isPublic,
                                    highlightColor.hexColor.takeUnless { isSelected },
                                )
                            }
                        },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onDismissRequest,
                    enabled = !draft.isSaving,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            disabledContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                ) {
                    Text(stringResource(UiR.string.cancel))
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onSave,
                    enabled = !draft.isSaving,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background,
                            disabledContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.background,
                        ),
                    modifier = Modifier.testTag("note_save_button"),
                ) {
                    if (draft.isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(UiR.string.notes_save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (draft.showsDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = onCancelDiscard,
            title = { Text(stringResource(UiR.string.notes_discard_title)) },
            text = { Text(stringResource(UiR.string.notes_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDiscard,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(UiR.string.notes_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDiscard) { Text(stringResource(UiR.string.cancel)) }
            },
        )
    }

    if (draft.showsSaveError) {
        AlertDialog(
            onDismissRequest = onDismissSaveError,
            title = { Text(stringResource(UiR.string.error)) },
            text = { Text(stringResource(UiR.string.generic_request_error_body)) },
            confirmButton = {
                TextButton(onClick = onDismissSaveError) { Text(stringResource(UiR.string.ok)) }
            },
        )
    }
}

@Composable
private fun NoteHeader(
    referenceTitle: String,
    isPublic: Boolean,
    isEnabled: Boolean,
    onToggleVisibility: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = BookmarkIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(UiR.string.notes_save),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = referenceTitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier =
                Modifier
                    .heightIn(min = 48.dp)
                    .alpha(if (isEnabled) 1f else 0.5f)
                    .toggleable(
                        value = isPublic,
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = isEnabled,
                        role = Role.Switch,
                        onValueChange = { onToggleVisibility() },
                    ).testTag("note_visibility_toggle"),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .indication(interactionSource, ripple())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = if (isPublic) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(if (isPublic) UiR.string.notes_public else UiR.string.notes_private),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun NoteTextField(
    content: String,
    isEnabled: Boolean,
    onContentChange: (String) -> Unit,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground)
    val label = stringResource(UiR.string.notes_placeholder)
    BasicTextField(
        value = content,
        onValueChange = onContentChange,
        enabled = isEnabled,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        maxLines = 3,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label }
                .testTag("note_content_field"),
        decorationBox = { innerTextField ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (content.isEmpty()) {
                    Text(
                        text = label,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                innerTextField()
            }
        },
    )
}
