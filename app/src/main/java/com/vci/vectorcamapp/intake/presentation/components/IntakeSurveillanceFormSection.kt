package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.form.ToggleField
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.presentation.IntakeAction
import com.vci.vectorcamapp.intake.presentation.IntakeState

@Composable
internal fun IntakeSurveillanceFormSection(
    state: IntakeState,
    surveillanceForm: SurveillanceForm,
    onAction: (IntakeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    IntakeTile(
        title = stringResource(R.string.intake_title_surveillance_form),
        iconPainter = painterResource(id = R.drawable.ic_clipboard),
        iconDescription = stringResource(R.string.intake_content_description_surveillance_form),
        modifier = modifier,
    ) {
        HouseholdFields(surveillanceForm = surveillanceForm, state = state, onAction = onAction)
        LlinFields(surveillanceForm = surveillanceForm, state = state, onAction = onAction)
    }
}

@Composable
private fun HouseholdFields(
    surveillanceForm: SurveillanceForm,
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
) {
    TextEntryField(
        label = stringResource(R.string.intake_label_num_people_house),
        required = true,
        value = surveillanceForm.numPeopleSleptInHouse.toIntakeCountText(),
        onValueChange = { onAction(IntakeAction.EnterNumPeopleSleptInHouse(it)) },
        singleLine = true,
        error = state.intakeErrors.numPeopleSleptInHouse,
    )
    TextEntryField(
        label = stringResource(R.string.intake_label_num_children_under_5),
        required = true,
        value = surveillanceForm.numChildrenUnder5?.toString().orEmpty(),
        onValueChange = { onAction(IntakeAction.EnterNumChildrenUnder5(it)) },
        singleLine = true,
        error = state.intakeErrors.numChildrenUnder5,
    )
    ToggleField(
        label = stringResource(R.string.intake_label_has_pregnant_woman),
        checked = surveillanceForm.hasPregnantWoman == true,
        onCheckedChange = { onAction(IntakeAction.ToggleHasPregnantWoman(it)) },
    )
    ToggleField(
        label = stringResource(R.string.intake_label_irs_conducted),
        checked = surveillanceForm.wasIrsConducted,
        onCheckedChange = { onAction(IntakeAction.ToggleIrsConducted(it)) },
    )
    surveillanceForm.monthsSinceIrs?.let { monthsSinceIrs ->
        TextEntryField(
            label = stringResource(R.string.intake_label_months_since_irs),
            required = true,
            value = monthsSinceIrs.toIntakeCountText(),
            onValueChange = { onAction(IntakeAction.EnterMonthsSinceIrs(it)) },
            singleLine = true,
            error = state.intakeErrors.monthsSinceIrs,
        )
    }
}

@Composable
private fun LlinFields(
    surveillanceForm: SurveillanceForm,
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
) {
    TextEntryField(
        label = stringResource(R.string.intake_label_num_llins),
        required = true,
        value = surveillanceForm.numLlinsAvailable.toIntakeCountText(),
        onValueChange = { onAction(IntakeAction.EnterNumLlinsAvailable(it)) },
        singleLine = true,
        error = state.intakeErrors.numLlinsAvailable,
    )
    surveillanceForm.llinType?.let { current ->
        DropdownField(
            label = stringResource(R.string.intake_label_llin_type),
            required = true,
            options = IntakeDropdownOptions.LlinTypeOption.entries,
            selectedOption = IntakeDropdownOptions.LlinTypeOption.entries.firstOrNull { it.label == current },
            onOptionSelected = { onAction(IntakeAction.SelectLlinType(it)) },
            error = state.intakeErrors.llinType,
        ) { llinType ->
            DropdownOptionLabel(text = llinType.label)
        }
    }
    surveillanceForm.llinBrand?.let { current ->
        DropdownField(
            label = stringResource(R.string.intake_label_llin_brand),
            required = true,
            options = IntakeDropdownOptions.LlinBrandOption.entries.filter {
                it.type?.label == surveillanceForm.llinType || it.type == null
            },
            selectedOption = IntakeDropdownOptions.LlinBrandOption.entries.firstOrNull { it.label == current },
            onOptionSelected = { onAction(IntakeAction.SelectLlinBrand(it)) },
            error = state.intakeErrors.llinBrand,
        ) { llinBrand ->
            DropdownOptionLabel(text = llinBrand.label)
        }
    }
    surveillanceForm.numPeopleSleptUnderLlin?.let { numPeopleSleptUnderLlin ->
        TextEntryField(
            label = stringResource(R.string.intake_label_num_people_llin),
            required = true,
            value = numPeopleSleptUnderLlin.toIntakeCountText(),
            onValueChange = { onAction(IntakeAction.EnterNumPeopleSleptUnderLlin(it)) },
            singleLine = true,
            error = state.intakeErrors.numPeopleSleptUnderLlin,
        )
    }
}

private fun Int.toIntakeCountText(): String = if (this < 0) "" else toString()
