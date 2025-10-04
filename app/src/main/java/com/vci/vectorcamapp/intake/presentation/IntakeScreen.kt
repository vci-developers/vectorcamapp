package com.vci.vectorcamapp.intake.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.form.DatePickerField
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.form.ToggleField
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.core.presentation.components.pill.InfoPill
import com.vci.vectorcamapp.core.presentation.util.error.toString
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.domain.util.IntakeError
import com.vci.vectorcamapp.intake.presentation.components.IntakeTile
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.core.presentation.extensions.displayText
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun IntakeScreen(
    state: IntakeState, onAction: (IntakeAction) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BackHandler {
        when (state.session.type) {
            SessionType.SURVEILLANCE -> onAction(IntakeAction.ReturnToLandingScreen)
            SessionType.DATA_COLLECTION -> onAction(IntakeAction.ReturnToSettingsScreen)
        }
    }

    ScreenHeader(
        title = "${state.session.type.displayText(context)} Intake",
        subtitle = "Please fill out the information below",
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Back Button",
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeMedium)
                    .clickable {
                        when (state.session.type) {
                            SessionType.SURVEILLANCE -> onAction(IntakeAction.ReturnToLandingScreen)
                            SessionType.DATA_COLLECTION -> onAction(IntakeAction.ReturnToSettingsScreen)
                        }
                    }
            )
        },
        modifier = modifier
    ) {
        item {
            IntakeTile(
                title = "General Information",
                iconPainter = painterResource(R.drawable.ic_info),
                iconDescription = "General Information Icon"
            ) {
                TextEntryField(
                    label = "Collector Name",
                    value = state.session.collectorName,
                    onValueChange = { onAction(IntakeAction.EnterCollectorName(it)) },
                    singleLine = true,
                    error = state.intakeErrors.collectorName
                )

                TextEntryField(
                    label = "Collector Title",
                    value = state.session.collectorTitle,
                    onValueChange = { onAction(IntakeAction.EnterCollectorTitle(it)) },
                    singleLine = true,
                    error = state.intakeErrors.collectorTitle
                )

                DatePickerField(
                    label = "Collection Date",
                    selectedDateInMillis = state.session.collectionDate,
                    onDateSelected = { onAction(IntakeAction.PickCollectionDate(it)) },
                    error = state.intakeErrors.collectionDate,
                    modifier = Modifier.fillMaxWidth()
                )

                val isOtherCollectionMethod =
                    state.session.collectionMethod.startsWith(
                        IntakeDropdownOptions.CollectionMethodOption.OTHER.label,
                        ignoreCase = true
                    )

                DropdownField(
                    label = "Collection Method",
                    options = IntakeDropdownOptions.CollectionMethodOption.entries,
                    selectedOption = if (isOtherCollectionMethod)
                        IntakeDropdownOptions.CollectionMethodOption.OTHER
                    else
                        IntakeDropdownOptions.CollectionMethodOption.entries.firstOrNull { it.label == state.session.collectionMethod },
                    onOptionSelected = { onAction(IntakeAction.UpdateCollectionMethod(it.label)) },
                    error = state.intakeErrors.collectionMethod,
                    modifier = Modifier.fillMaxWidth()
                ) { collectionMethod ->
                    Text(
                        text = collectionMethod.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                if (isOtherCollectionMethod) {
                    TextEntryField(
                        label = "Other Collection Method",
                        value = state.session.collectionMethod.removePrefix(IntakeDropdownOptions.CollectionMethodOption.OTHER.label)
                            .trimStart(),
                        onValueChange = { onAction(IntakeAction.UpdateCollectionMethod("${IntakeDropdownOptions.CollectionMethodOption.OTHER.label} $it")) },
                        singleLine = true,
                        error = state.intakeErrors.collectionMethod
                    )
                }

                val isOtherSpecimenCondition =
                    state.session.specimenCondition.startsWith(
                        IntakeDropdownOptions.SpecimenConditionOption.OTHER.label,
                        ignoreCase = true
                    )

                DropdownField(
                    label = "Specimen Condition",
                    options = IntakeDropdownOptions.SpecimenConditionOption.entries,
                    selectedOption = if (isOtherSpecimenCondition)
                        IntakeDropdownOptions.SpecimenConditionOption.OTHER
                    else
                        IntakeDropdownOptions.SpecimenConditionOption.entries.firstOrNull { it.label == state.session.specimenCondition },
                    onOptionSelected = { onAction(IntakeAction.UpdateSpecimenCondition(it.label)) },
                    error = state.intakeErrors.specimenCondition,
                    modifier = Modifier.fillMaxWidth()
                ) { specimenCondition ->
                    Text(
                        text = specimenCondition.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                if (isOtherSpecimenCondition) {
                    TextEntryField(
                        label = "Other Specimen Condition",
                        value = state.session.specimenCondition.removePrefix(IntakeDropdownOptions.SpecimenConditionOption.OTHER.label)
                            .trimStart(),
                        onValueChange = { onAction(IntakeAction.UpdateSpecimenCondition("${IntakeDropdownOptions.SpecimenConditionOption.OTHER.label} $it")) },
                        singleLine = true,
                        error = state.intakeErrors.specimenCondition
                    )
                }
            }
        }

        item {
            IntakeTile(
                title = "Geographical Information",
                iconPainter = painterResource(id = R.drawable.ic_pin),
                iconDescription = "Geographical Information Icon"
            ) {
                DropdownField(
                    label = "District",
                    options = state.allSitesInProgram.map { it.district }.distinct(),
                    selectedOption = state.selectedDistrict,
                    onOptionSelected = { onAction(IntakeAction.SelectDistrict(it)) },
                    error = state.intakeErrors.district,
                ) { district ->
                    Text(
                        text = district,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                if (state.selectedDistrict.isNotBlank()) {
                    DropdownField(
                        label = "Village Name",
                        options = state.allSitesInProgram.filter { it.district == state.selectedDistrict }
                            .map { it.villageName }.distinct(),
                        selectedOption = state.selectedVillageName,
                        onOptionSelected = {
                            onAction(IntakeAction.SelectVillageName(it))
                        },
                        error = state.intakeErrors.villageName,
                    ) { villageName ->
                        Text(
                            text = villageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }
                }

                if (state.selectedVillageName.isNotBlank()) {
                    DropdownField(
                        label = "House Number",
                        options = state.allSitesInProgram.filter { it.district == state.selectedDistrict && it.villageName == state.selectedVillageName }
                            .map { it.houseNumber }.distinct(),
                        selectedOption = state.selectedHouseNumber,
                        onOptionSelected = { onAction(IntakeAction.SelectHouseNumber(it)) },
                        error = state.intakeErrors.houseNumber,
                    ) { houseNumber ->
                        Text(
                            text = houseNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }
                }

                state.surveillanceForm?.let { surveillanceForm ->
                    TextEntryField(
                        label = "Number of People Living in the House",
                        value = if (surveillanceForm.numPeopleSleptInHouse == 0) "" else surveillanceForm.numPeopleSleptInHouse.toString(),
                        onValueChange = { onAction(IntakeAction.EnterNumPeopleSleptInHouse(it.filter { character -> character.isDigit() })) },
                        placeholder = "0",
                        singleLine = true,
                    )
                }

                when {
                    state.session.latitude != null && state.session.longitude != null -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            InfoPill(
                                text = "Latitude: ${state.session.latitude}",
                                color = MaterialTheme.colors.info,
                                modifier = Modifier.weight(1f)
                            )
                            InfoPill(
                                text = "Longitude: ${state.session.longitude}",
                                color = MaterialTheme.colors.info,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    state.locationError != null -> {
                        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall)) {
                            Text(
                                text = "Could not get location: ${
                                    state.locationError.toString(
                                        context
                                    )
                                }",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.error
                            )

                            if (state.locationError == IntakeError.LOCATION_GPS_TIMEOUT) {
                                ActionButton(
                                    label = "Retry Location",
                                    onClick = { onAction(IntakeAction.RetryLocation) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    else -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colors.secondary,
                                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium)
                            )
                            Text(
                                "Getting location…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        state.surveillanceForm?.let { surveillanceForm ->
            item {
                IntakeTile(
                    title = "Surveillance Form",
                    iconPainter = painterResource(id = R.drawable.ic_clipboard),
                    iconDescription = "Surveillance Form Icon"
                ) {
                    ToggleField(
                        label = "Was IRS conducted in this household?",
                        checked = surveillanceForm.wasIrsConducted,
                        onCheckedChange = {
                            onAction(
                                IntakeAction.ToggleIrsConducted(
                                    it
                                )
                            )
                        })

                    if (surveillanceForm.wasIrsConducted) {
                        TextEntryField(
                            label = "Months Since IRS",
                            value = surveillanceForm.monthsSinceIrs?.let { if (it == 0) "" else it.toString() }
                                .orEmpty(),
                            onValueChange = {
                                onAction(IntakeAction.EnterMonthsSinceIrs(it.filter { character -> character.isDigit() }))
                            },
                            placeholder = "0",
                            singleLine = true,
                        )
                    }

                    TextEntryField(
                        label = "Number of LLINs Available",
                        value = if (surveillanceForm.numLlinsAvailable == 0) "" else surveillanceForm.numLlinsAvailable.toString(),
                        onValueChange = { onAction(IntakeAction.EnterNumLlinsAvailable(it.filter { character -> character.isDigit() })) },
                        placeholder = "0",
                        singleLine = true,
                    )

                    surveillanceForm.llinType?.let { current ->
                        DropdownField(
                            label = "LLIN Type",
                            options = IntakeDropdownOptions.LlinTypeOption.entries,
                            selectedOption = IntakeDropdownOptions.LlinTypeOption.entries.firstOrNull { it.label == current },
                            onOptionSelected = {
                                onAction(IntakeAction.SelectLlinType(it))
                            },
                            error = state.intakeErrors.llinType,
                        ) { llinType ->
                            Text(
                                text = llinType.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.textPrimary
                            )
                        }
                    }

                    surveillanceForm.llinBrand?.let { current ->
                        DropdownField(
                            label = "LLIN Brand",
                            options = IntakeDropdownOptions.LlinBrandOption.entries,
                            selectedOption = IntakeDropdownOptions.LlinBrandOption.entries.firstOrNull { it.label == current },
                            onOptionSelected = {
                                onAction(IntakeAction.SelectLlinBrand(it))
                            },
                            error = state.intakeErrors.llinBrand,
                        ) { llinBrand ->
                            Text(
                                text = llinBrand.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.textPrimary
                            )
                        }
                    }

                    surveillanceForm.numPeopleSleptUnderLlin?.let { current ->
                        TextEntryField(
                            label = "Number of People who Slept Under LLIN",
                            value = if (current == 0) "" else current.toString(),
                            onValueChange = {
                                onAction(IntakeAction.EnterNumPeopleSleptUnderLlin(it.filter { character -> character.isDigit() }))
                            },
                            placeholder = "0",
                            singleLine = true,
                        )
                    }
                }
            }
        }

        item {
            IntakeTile(
                title = "Additional Notes",
                iconPainter = painterResource(id = R.drawable.ic_notes),
                iconDescription = "Additional Notes Icon"
            ) {
                TextEntryField(
                    label = "Notes",
                    value = state.session.notes,
                    onValueChange = { onAction(IntakeAction.EnterNotes(it)) },
                    placeholder = "1000 character limit...",
                )
            }
        }

        item {
            ActionButton(
                label = "Begin ${state.session.type.displayText(context)} Imaging",
                onClick = { onAction(IntakeAction.SubmitIntakeForm) },
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium)
            )
        }
    }
}

@PreviewLightDark
@Composable
fun SurveillanceFormScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            IntakeScreen(
                state = IntakeState(), onAction = { }, modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
