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
import com.vci.vectorcamapp.core.presentation.components.form.DatePickerField
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
    val label = question.label
    when (question.type) {
        "text" -> {
            TextEntryField(
                label = label,
                required = question.required,
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                error = error,
                modifier = modifier
            )
        }

        "number" -> {
            TextEntryField(
                label = label,
                required = question.required,
                value = value,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d+\\.?\\d{0,2}$"))) {
                        onValueChange(newValue)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                error = error,
                modifier = modifier
            )
        }

        "boolean" -> {
            ToggleField(
                label = label,
                required = question.required,
                checked = value.toBooleanStrictOrNull() ?: false,
                onCheckedChange = { onValueChange(it.toString()) }
            )
        }
        
        "date" -> {
            DatePickerField(
                label = label,
                required = question.required,
                selectedDateInMillis = value.toLongOrNull(),
                onDateSelected = { onValueChange(it.toString()) },
                error = error,
                modifier = modifier.fillMaxWidth()
            )
        }

        "select" -> {
            val options = question.options.orEmpty()
            DropdownField(
                label = label,
                required = question.required,
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