package com.vci.vectorcamapp.surveillance_form.presentation

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(verticalScrollState)
    ) {
        TextEntryField(
            label = "Country",
            value = state.surveillanceForm.country,
            onValueChange = { onAction(SurveillanceFormAction.EnterCountry(it)) },
            singleLine = true,
            error = state.surveillanceFormErrors.country,
            modifier = modifier
        )

        TextEntryField(
            label = "District",
            value = state.surveillanceForm.district,
            onValueChange = { onAction(SurveillanceFormAction.EnterDistrict(it)) },
            singleLine = true,
            error = state.surveillanceFormErrors.district,
            modifier = modifier
        )

        TextEntryField(
            label = "Health Center",
            value = state.surveillanceForm.healthCenter,
            onValueChange = { onAction(SurveillanceFormAction.EnterHealthCenter(it)) },
            singleLine = true,
            error = state.surveillanceFormErrors.healthCenter,
            modifier = modifier
        )

        TextEntryField(
            label = "Sentinel Site",
            value = state.surveillanceForm.sentinelSite,
            onValueChange = { onAction(SurveillanceFormAction.EnterSentinelSite(it)) },
            singleLine = true,
            error = state.surveillanceFormErrors.sentinelSite,
            modifier = modifier
        )

        TextEntryField(
            label = "Household Number",
            value = state.surveillanceForm.householdNumber,
            onValueChange = { onAction(SurveillanceFormAction.EnterHouseholdNumber(it)) },
            singleLine = true,
            error = state.surveillanceFormErrors.householdNumber,
            modifier = modifier
        )

        Text("Latitude: %.4f".format(state.surveillanceForm.latitude))
        Text("Longitude: %.4f".format(state.surveillanceForm.longitude))

        DatePickerField(
            label = "Collection Date",
            selectedDateInMillis = state.surveillanceForm.collectionDate,
            onDateSelected = { onAction(SurveillanceFormAction.PickCollectionDate(it)) },
            error = state.surveillanceFormErrors.collectionDate,
            modifier = modifier
        )

        DropdownField(
            label = "Collection Method",
            options = CollectionMethodOption.entries,
            selectedOption = CollectionMethodOption.entries.find { it.label == state.surveillanceForm.collectionMethod },
            onOptionSelected = { onAction(SurveillanceFormAction.SelectCollectionMethod(it)) },
            error = state.surveillanceFormErrors.collectionMethod,
            modifier = modifier
        )

        TextEntryField(
            label = "Collector Name",
            value = state.surveillanceForm.collectorName,
            onValueChange = { onAction(SurveillanceFormAction.EnterCollectorName(it)) },
            singleLine = true,
            error = state.surveillanceFormErrors.collectorName,
            modifier = modifier
        )

        TextEntryField(
            label = "Collector Title",
            value = state.surveillanceForm.collectorTitle,
            onValueChange = { onAction(SurveillanceFormAction.EnterCollectorTitle(it)) },
            singleLine = true,
            error = state.surveillanceFormErrors.collectorTitle,
            modifier = modifier
        )

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

        TextEntryField(
            label = "Notes",
            value = state.surveillanceForm.notes ,
            onValueChange = { onAction(SurveillanceFormAction.EnterNotes(it)) },
            modifier = modifier
        )

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
