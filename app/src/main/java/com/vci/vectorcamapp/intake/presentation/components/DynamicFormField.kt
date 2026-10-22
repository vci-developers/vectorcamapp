package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.presentation.components.form.DatePickerField
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.MultiSelectField
import com.vci.vectorcamapp.core.presentation.components.form.QrCodeField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.form.ToggleField
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.program.kenya.domain.KenyaFormRules

@Composable
fun DynamicFormField(
    question: FormQuestion,
    value: FormAnswerValue,
    onValueChange: (FormAnswerValue) -> Unit,
    modifier: Modifier = Modifier,
    error: Error? = null,
) {
    val label = question.label
    val text = (value as? FormAnswerValue.StringValue)?.value.orEmpty()
    val onTextChange: (String) -> Unit = { onValueChange(FormAnswerValue.StringValue(it)) }
    when (question.type) {
        "multi-select" -> MultiSelectField(
            label = label,
            required = question.required,
            options = question.options.orEmpty(),
            selectedValues = (value as? FormAnswerValue.ListValue)?.value.orEmpty(),
            onSelectionChange = { onValueChange(FormAnswerValue.ListValue(it)) },
            error = error,
            modifier = modifier,
        )
        "qr" -> {
            QrCodeField(
                label = label,
                required = question.required,
                value = text,
                onValueChange = onTextChange,
                error = error,
                modifier = modifier,
            )
        }

        "text" -> {
            TextEntryField(
                label = label,
                required = question.required,
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                error = error,
                modifier = modifier
            )
        }

        "number" -> {
            TextEntryField(
                label = label,
                required = question.required,
                value = text,
                onValueChange = { newValue ->
                    if ((newValue.isEmpty() || newValue.matches(Regex("^\\d+\\.?\\d{0,2}$"))) &&
                        KenyaFormRules.isWithinNumberLimit(question, FormAnswerValue.StringValue(newValue))) {
                        onTextChange(newValue)
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
                checked = text.toBooleanStrictOrNull() ?: false,
                onCheckedChange = { onTextChange(it.toString()) }
            )
        }

        "date" -> {
            DatePickerField(
                label = label,
                required = question.required,
                selectedDateInMillis = text.toLongOrNull(),
                onDateSelected = { onTextChange(it.toString()) },
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
                selectedOption = options.firstOrNull { it == text },
                onOptionSelected = onTextChange,
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
