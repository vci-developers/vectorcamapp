package com.vci.vectorcamapp.surveillance_form.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.surveillance_form.domain.enums.CollectionMethodOption
import com.vci.vectorcamapp.surveillance_form.domain.enums.LlinBrandOption
import com.vci.vectorcamapp.surveillance_form.domain.enums.LlinTypeOption
import com.vci.vectorcamapp.surveillance_form.presentation.components.DatePickerField
import com.vci.vectorcamapp.surveillance_form.presentation.components.DropdownField
import com.vci.vectorcamapp.surveillance_form.presentation.components.TextEntryField
import com.vci.vectorcamapp.surveillance_form.presentation.components.ToggleField
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun SurveillanceFormScreen(
    state: SurveillanceFormState,
    onAction: (SurveillanceFormAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()

    BackHandler {
        onAction(SurveillanceFormAction.SaveSessionProgress)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(verticalScrollState)
    ) {

        TextEntryField(
            label = "Number of People who Slept in the House",
            value = state.surveillanceForm.numPeopleSleptInHouse.toString(),
            onValueChange = { onAction(SurveillanceFormAction.EnterNumPeopleSleptInHouse(it)) },
            singleLine = true,
            keyboardType = KeyboardType.Number,
            modifier = modifier
        )

        ToggleField(
            label = "IRS Conducted in this Household",
            checked = state.surveillanceForm.wasIrsConducted,
            onCheckedChange = { onAction(SurveillanceFormAction.ToggleIrsConducted(it)) },
            modifier = modifier,
        )
        
        state.surveillanceForm.monthsSinceIrs?.let {
            TextEntryField(
                label = "Months Since IRS",
                value = state.surveillanceForm.monthsSinceIrs.toString(),
                onValueChange = { onAction(SurveillanceFormAction.EnterMonthsSinceIrs(it)) },
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = modifier
            )
        }

        TextEntryField(
            label = "Number of LLINs Available",
            value = state.surveillanceForm.numLlinsAvailable.toString(),
            onValueChange = { onAction(SurveillanceFormAction.EnterNumLlinsAvailable(it)) },
            singleLine = true,
            keyboardType = KeyboardType.Number,
            modifier = modifier
        )

        state.surveillanceForm.llinType?.let {
            DropdownField(
                label = "LLIN Type",
                options = LlinTypeOption.entries,
                selectedOption = LlinTypeOption.entries.find { it.label == state.surveillanceForm.llinType },
                onOptionSelected = { onAction(SurveillanceFormAction.SelectLlinType(it)) },
                error = state.surveillanceFormErrors.llinType,
                modifier = modifier
            )
        }

        state.surveillanceForm.llinBrand?.let {
            DropdownField(
                label = "LLIN Brand",
                options = LlinBrandOption.entries,
                selectedOption = LlinBrandOption.entries.find { it.label == state.surveillanceForm.llinBrand },
                onOptionSelected = { onAction(SurveillanceFormAction.SelectLlinBrand(it)) },
                error = state.surveillanceFormErrors.llinBrand,
                modifier = modifier
            )
        }

        state.surveillanceForm.numPeopleSleptUnderLlin?.let {
            TextEntryField(
                label = "Number of People who Slept Under LLIN",
                value = state.surveillanceForm.numPeopleSleptUnderLlin.toString(),
                onValueChange = { onAction(SurveillanceFormAction.EnterNumPeopleSleptUnderLlin(it)) },
                singleLine = true,
                keyboardType = KeyboardType.Number,
                modifier = modifier
            )
        }

        Button(
            onClick = { onAction(SurveillanceFormAction.SubmitSurveillanceForm) }, modifier = modifier
        ) {
            Text("Start Imaging")
        }
    }
}

@PreviewLightDark
@Composable
fun SurveillanceFormScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            SurveillanceFormScreen(
                state = SurveillanceFormState(),
                onAction = { },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
