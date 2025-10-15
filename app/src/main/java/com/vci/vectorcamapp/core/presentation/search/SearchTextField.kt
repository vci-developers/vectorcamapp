package com.vci.vectorcamapp.core.presentation.search

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.tooltip.Tooltip

@Composable
fun SearchTextField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isSingleLine: Boolean = true,
    isTooltipVisible: Boolean = false,
    onSearchSubmitted: (() -> Unit)? = null,
    onTooltipShow: (() -> Unit)? = null,
    onTooltipDismiss: (() -> Unit)? = null,
    tooltipButtonText: String = "Tap to learn more about searching",
    tooltipContent: (@Composable () -> Unit)? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    TextEntryField(
        value = searchQuery,
        onValueChange = { newSearchQueryText -> onSearchQueryChange(newSearchQueryText) },
        placeholder = placeholder,
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

    if (tooltipContent != null && onTooltipShow != null && onTooltipDismiss != null) {
        Tooltip(
            isVisible = isTooltipVisible,
            onClick = onTooltipShow,
            onDismiss = onTooltipDismiss,
            buttonText = tooltipButtonText,
            modifier = modifier
        ) {
            tooltipContent()
        }
    }
}
