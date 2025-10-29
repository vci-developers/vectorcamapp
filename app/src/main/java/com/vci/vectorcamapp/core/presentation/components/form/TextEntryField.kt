package com.vci.vectorcamapp.core.presentation.components.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.presentation.util.error.toString
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun TextEntryField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    singleLine: Boolean = false,
    error: Error? = null,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    maxCharacters: Int = 200,
    showErrorMessage: Boolean = true,
    changeTextColorOnError: Boolean = false,
) {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraExtraSmall),
        modifier = Modifier.fillMaxWidth()
    ) {
        label?.let {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textSecondary,
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                val hasValidCharacters = newValue.all { character ->
                    character.code <= 255 || character.isWhitespace()
                }
                if (hasValidCharacters && newValue.length <= maxCharacters) {
                    onValueChange(newValue)
                }
            },
            isError = error != null,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            placeholder = {
                if (placeholder != null) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textSecondary,
                    )
                }
            },
            maxLines = 8,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (error != null && changeTextColorOnError)
                    MaterialTheme.colors.error
                else
                    MaterialTheme.colors.textPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colors.transparent,
                unfocusedBorderColor = MaterialTheme.colors.transparent,
                errorBorderColor = MaterialTheme.colors.transparent,
                cursorColor = MaterialTheme.colors.primary,
                selectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.colors.primary,
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.25f)
                )
            ),
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colors.cardBackground,
                    shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)
                )
                .border(
                    width = MaterialTheme.dimensions.borderThicknessThick,
                    shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall),
                    color = if (error != null) MaterialTheme.colors.error else MaterialTheme.colors.primary
                )
                .heightIn(min = MaterialTheme.dimensions.componentHeightMedium)
        )

        if (error != null && showErrorMessage) {
            Text(
                text = error.toString(context),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colors.error
            )
        }
    }
}
