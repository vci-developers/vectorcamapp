package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import com.vci.vectorcamapp.collection_batch.domain.util.error.CollectionBatchFormError
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
    errorMessage: String? = null,
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
            val locationError = error == FormValidationError.INVALID_CUP_LOCATION ||
                error == CollectionBatchFormError.INVALID_CUP_LOCATION
            val locationMessage = if (locationError) {
                when (KenyaFormRules.cupLocationToken(question)) {
                    "IN" -> R.string.form_validation_error_cup_indoors
                    "OUT" -> R.string.form_validation_error_cup_outdoors
                    "ANS" -> R.string.form_validation_error_cup_animal_shelter
                    else -> null
                }
            } else null
            QrCodeField(
                label = label,
                required = question.required,
                value = text,
                onValueChange = onTextChange,
                error = error,
                showErrorMessage = locationMessage == null && errorMessage == null,
                modifier = modifier,
            )
            if (errorMessage != null || locationMessage != null) {
                Text(
                    text = errorMessage ?: stringResource(requireNotNull(locationMessage)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.error,
                )
            }
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
