package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.presentation.IntakeAction
import com.vci.vectorcamapp.intake.presentation.IntakeState

@Composable
internal fun SpecimenConditionField(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
) {
    val otherLabel = IntakeDropdownOptions.SpecimenConditionOption.OTHER.label
    val isOther = state.session.specimenCondition.startsWith(otherLabel, ignoreCase = true)
    val selected = if (isOther) {
        IntakeDropdownOptions.SpecimenConditionOption.OTHER
    } else {
        IntakeDropdownOptions.SpecimenConditionOption.entries.firstOrNull {
            it.label == state.session.specimenCondition
        }
    }

    DropdownField(
        label = stringResource(R.string.intake_label_specimen_condition),
        required = true,
        options = IntakeDropdownOptions.SpecimenConditionOption.entries,
        selectedOption = selected,
        onOptionSelected = { onAction(IntakeAction.UpdateSpecimenCondition(it.label)) },
        error = state.intakeErrors.specimenCondition,
        modifier = Modifier.fillMaxWidth(),
    ) { specimenCondition ->
        DropdownOptionLabel(text = specimenCondition.label)
    }

    if (isOther) {
        OtherLabeledField(
            label = stringResource(R.string.intake_label_other_specimen_condition),
            prefix = otherLabel,
            value = state.session.specimenCondition,
            error = state.intakeErrors.specimenCondition,
            onValueChange = { onAction(IntakeAction.UpdateSpecimenCondition("$otherLabel $it")) },
        )
    }
}

@Composable
internal fun OtherLabeledField(
    label: String,
    prefix: String,
    value: String,
    error: Error?,
    onValueChange: (String) -> Unit,
) {
    TextEntryField(
        label = label,
        required = true,
        value = value.removePrefix(prefix).trimStart(),
        onValueChange = onValueChange,
        singleLine = true,
        error = error,
    )
}
