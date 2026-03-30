package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.form.ToggleField
import com.vci.vectorcamapp.ui.extensions.colors

@Composable
fun DynamicFormField(
    question: FormQuestion,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: Error? = null,
) {
    when (question.type) {
        "text" -> {
            TextEntryField(
                label = question.label,
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                error = error,
                modifier = modifier
            )
        }

        "number" -> {
            TextEntryField(
                label = question.label,
                value = value,
                onValueChange = { newValue -> onValueChange(newValue.filter { it.isDigit() }) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                error = error,
                modifier = modifier
            )
        }

        "boolean" -> {
            ToggleField(
                label = question.label,
                checked = value.toBooleanStrictOrNull() ?: false,
                onCheckedChange = { onValueChange(it.toString()) }
            )
        }

        "select" -> {
            val options = question.options.orEmpty()
            DropdownField(
                label = question.label,
                options = options,
                selectedOption = options.firstOrNull { it == value },
                onOptionSelected = onValueChange,
                error = error,
                modifier = modifier.fillMaxWidth()
            ) { option ->
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.textPrimary
                )
            }
        }
    }
}