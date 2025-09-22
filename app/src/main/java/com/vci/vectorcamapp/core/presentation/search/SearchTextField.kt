package com.vci.vectorcamapp.core.presentation.components.form

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

@Composable
fun SearchTextField(
    searchQueryText: String,
    onSearchQueryTextChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    isSingleLine: Boolean = true,
    onSearchSubmitted: (() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    TextEntryField(
        value = searchQueryText,
        onValueChange = { newSearchQueryText -> onSearchQueryTextChange(newSearchQueryText) },
        placeholder = placeholderText,
        modifier = modifier,
        singleLine = isSingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearchSubmitted?.invoke()
                keyboardController?.hide()
            }
        )
    )
}
