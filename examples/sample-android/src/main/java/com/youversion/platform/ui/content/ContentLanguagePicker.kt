package com.youversion.platform.ui.content

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.Collator

/**
 * A sheet that lists the content languages by name, with a search field, and checks the selected one.
 *
 * @param selectedLanguageTag The current content language.
 * @param onSelect Called with the language the user picks; the sheet then closes.
 * @param onDismiss Called when the sheet closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContentLanguagePicker(
    selectedLanguageTag: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val sortedTags =
        remember {
            val collator = Collator.getInstance()
            CONTENT_LANGUAGE_TAGS.sortedWith(compareBy(collator) { contentLanguageName(it) })
        }
    val search = query.trim()
    val tags =
        sortedTags.filter {
            search.isEmpty() || contentLanguageName(it).contains(search, ignoreCase = true) ||
                it.contains(search, ignoreCase = true)
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Content language") },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search languages") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = CircleShape,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (tags.isEmpty()) {
            Text(
                text = "No results for “$search”",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(modifier = Modifier.selectableGroup()) {
            items(tags, key = { it }) { tag ->
                val isSelected = tag == selectedLanguageTag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = isSelected, role = Role.RadioButton) {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    onSelect(tag)
                                    onDismiss()
                                }
                            }.padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = contentLanguageName(tag),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                }
            }
        }
    }
}
