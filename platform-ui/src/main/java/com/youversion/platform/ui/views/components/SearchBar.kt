package com.youversion.platform.ui.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.core.utilities.graphemeClusterCount
import com.youversion.platform.ui.R
import com.youversion.platform.ui.theme.readerColorScheme
import com.youversion.platform.ui.theme.ui.BibleReaderTheme

/** A styled search text field with a search icon and placeholder text. */
@Composable
@PlatformInternalApi
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.readerColorScheme.buttonPrimaryColor,
    onSubmit: (() -> Unit)? = null,
    showsClearButton: Boolean = false,
    clearButtonContentDescription: String = stringResource(R.string.clear_search),
    focusRequester: FocusRequester = remember { FocusRequester() },
    maximumGraphemeClusterCount: Int? = null,
) {
    BasicTextField(
        value = query,
        onValueChange = { newQuery ->
            if (maximumGraphemeClusterCount == null ||
                newQuery.graphemeClusterCount <= maximumGraphemeClusterCount
            ) {
                onQueryChange(newQuery)
            }
        },
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = if (onSubmit != null) ImeAction.Search else ImeAction.Unspecified,
            ),
        keyboardActions = KeyboardActions(onSearch = { onSubmit?.invoke() }),
        textStyle =
            BibleReaderTheme.typography.paragraphL.copy(
                color = MaterialTheme.readerColorScheme.readerTextPrimaryColor,
            ),
        cursorBrush = SolidColor(MaterialTheme.readerColorScheme.readerTextPrimaryColor),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(containerColor)
                        .padding(start = 16.dp, end = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.readerColorScheme.readerTextMutedColor,
                    modifier = Modifier.padding(vertical = 10.dp).size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search),
                            style = BibleReaderTheme.typography.paragraphL,
                            color = MaterialTheme.readerColorScheme.readerTextMutedColor,
                        )
                    }
                    innerTextField()
                }
                if (showsClearButton && query.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = ClearSearch,
                        contentDescription = clearButtonContentDescription,
                        tint = MaterialTheme.readerColorScheme.readerTextMutedColor,
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .clickable { onQueryChange("") }
                                .padding(10.dp)
                                .size(20.dp),
                    )
                }
            }
        },
    )
}
