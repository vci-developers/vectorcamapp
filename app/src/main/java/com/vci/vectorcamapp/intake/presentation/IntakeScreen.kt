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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.R
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
import com.vci.vectorcamapp.intake.presentation.util.IntakeTestTags
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun IntakeScreen(
    state: IntakeState, onAction: (IntakeAction) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BackHandler {
        onAction(IntakeAction.ReturnToLandingScreen)
    }

    ScreenHeader(
        title = "Session Intake",
        subtitle = "Please fill out the information below",
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Back Button",
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeMedium)
                    .clickable {
                        onAction(IntakeAction.ReturnToLandingScreen)
                    }
                    .testTag(IntakeTestTags.BACK_ICON)
            )
        },
        modifier = modifier.testTag(IntakeTestTags.INTAKE_SCREEN)
    ) {
        item {
            IntakeTile(
                title = "General Information",
                iconPainter = painterResource(R.drawable.ic_info),
                iconDescription = "General Information Icon",
                modifier = Modifier.testTag(IntakeTestTags.TILE_GENERAL)

            ) {
                InfoPill(
                    text = "Session Type: ${state.session.type.name}",
                    color = MaterialTheme.colors.info
                )

                TextEntryField(
                    label = "Collector Name",
                    value = state.session.collectorName,
                    onValueChange = { onAction(IntakeAction.EnterCollectorName(it)) },
                    singleLine = true,
                    error = state.intakeErrors.collectorName,
                    modifier = Modifier.testTag(IntakeTestTags.COLLECTOR_NAME)
                )

                TextEntryField(
                    label = "Collector Title",
                    value = state.session.collectorTitle,
                    onValueChange = { onAction(IntakeAction.EnterCollectorTitle(it)) },
                    singleLine = true,
                    error = state.intakeErrors.collectorTitle,
                    modifier = Modifier.testTag(IntakeTestTags.COLLECTOR_TITLE)
                )

                DatePickerField(
                    label = "Collection Date",
                    selectedDateInMillis = state.session.collectionDate,
                    onDateSelected = { onAction(IntakeAction.PickCollectionDate(it)) },
                    error = state.intakeErrors.collectionDate,
                    modifier = Modifier.fillMaxWidth()
                        .testTag(IntakeTestTags.COLLECTION_DATE)
                )

                val isOtherCollectionMethod =
                    state.session.collectionMethod.startsWith(IntakeDropdownOptions.CollectionMethodOption.OTHER.label, ignoreCase = true)

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
                        .testTag(IntakeTestTags.COLLECTION_METHOD_DD)
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
                        value = state.session.collectionMethod.removePrefix(IntakeDropdownOptions.CollectionMethodOption.OTHER.label).trimStart(),
                        onValueChange = { onAction(IntakeAction.UpdateCollectionMethod("${IntakeDropdownOptions.CollectionMethodOption.OTHER.label} $it")) },
                        singleLine = true,
                        error = state.intakeErrors.collectionMethod
                    )
                }

                val isOtherSpecimenCondition =
                    state.session.specimenCondition.startsWith(IntakeDropdownOptions.SpecimenConditionOption.OTHER.label, ignoreCase = true)

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
                        .testTag(IntakeTestTags.SPECIMEN_CONDITION_DD)
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
                        value = state.session.specimenCondition.removePrefix(IntakeDropdownOptions.SpecimenConditionOption.OTHER.label).trimStart(),
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
                iconDescription = "Geographical Information Icon",
                modifier = Modifier.testTag(IntakeTestTags.TILE_GEOGRAPHICAL)
            ) {
                DropdownField(
                    label = "District",
                    options = state.allSitesInProgram.map { it.district }.distinct(),
                    selectedOption = state.selectedDistrict,
                    onOptionSelected = { onAction(IntakeAction.SelectDistrict(it)) },
                    error = state.intakeErrors.district,
                    modifier = Modifier.testTag(IntakeTestTags.DISTRICT_DD)
                ) { district ->
                    Text(
                        text = district,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                if (state.selectedDistrict.isNotBlank()) {
                    DropdownField(
                        label = "Sentinel Site",
                        options = state.allSitesInProgram.filter { it.district == state.selectedDistrict }
                            .map { it.sentinelSite }.distinct(),
                        selectedOption = state.selectedSentinelSite,
                        onOptionSelected = {
                            onAction(IntakeAction.SelectSentinelSite(it))
                        },
                        error = state.intakeErrors.sentinelSite,
                        modifier = Modifier.testTag(IntakeTestTags.SENTINEL_SITE_DD)
                        ) { sentinelSite ->
                        Text(
                            text = sentinelSite,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }
                }

                TextEntryField(
                    label = "House Number",
                    value = state.session.houseNumber,
                    onValueChange = { onAction(IntakeAction.EnterHouseNumber(it)) },
                    singleLine = true,
                    error = state.intakeErrors.houseNumber,
                    modifier = Modifier.testTag(IntakeTestTags.HOUSE_NUMBER)
                )

                state.surveillanceForm?.let { surveillanceForm ->
                    TextEntryField(
                        label = "Number of People Living in the House",
                        value = if (surveillanceForm.numPeopleSleptInHouse == 0) "" else surveillanceForm.numPeopleSleptInHouse.toString(),
                        onValueChange = { onAction(IntakeAction.EnterNumPeopleSleptInHouse(it.filter { character -> character.isDigit() })) },
                        placeholder = "0",
                        singleLine = true,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.testTag(IntakeTestTags.NUM_PEOPLE_IN_HOUSE)
                    )
                }

                when {
                    state.session.latitude != null && state.session.longitude != null -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                            modifier = Modifier.fillMaxWidth()
                                .testTag(IntakeTestTags.LOCATION_COORDS_ROW)
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
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
                            modifier = Modifier.testTag(IntakeTestTags.LOCATION_ERROR_TEXT)
                        ) {
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
                                        .testTag(IntakeTestTags.LOCATION_RETRY_BUTTON)
                                )
                            }
                        }
                    }

                    else -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .testTag(IntakeTestTags.LOCATION_LOADING_ROW)
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
                    iconDescription = "Surveillance Form Icon",
                    modifier = Modifier.testTag(IntakeTestTags.TILE_FORM)
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
                        },
                        modifier = Modifier.testTag(IntakeTestTags.IRS_TOGGLE))

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
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.testTag(IntakeTestTags.MONTHS_SINCE_IRS)
                        )
                    }

                    TextEntryField(
                        label = "Number of LLINs Available",
                        value = if (surveillanceForm.numLlinsAvailable == 0) "" else surveillanceForm.numLlinsAvailable.toString(),
                        onValueChange = { onAction(IntakeAction.EnterNumLlinsAvailable(it.filter { character -> character.isDigit() })) },
                        placeholder = "0",
                        singleLine = true,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.testTag(IntakeTestTags.LLINS_AVAILABLE)
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
                            modifier = Modifier.testTag(IntakeTestTags.LLIN_TYPE_DD)
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
                            modifier = Modifier.testTag(IntakeTestTags.LLIN_BRAND_DD)
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
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.testTag(IntakeTestTags.NUM_PEOPLE_UNDER_LLIN)
                        )
                    }
                }
            }
        }

        item {
            IntakeTile(
                title = "Additional Notes",
                iconPainter = painterResource(id = R.drawable.ic_notes),
                iconDescription = "Additional Notes Icon",
                modifier = Modifier.testTag(IntakeTestTags.TILE_NOTES)
            ) {
                TextEntryField(
                    label = "Notes",
                    value = state.session.notes,
                    onValueChange = { onAction(IntakeAction.EnterNotes(it)) },
                    modifier = Modifier.testTag(IntakeTestTags.NOTES))
            }
        }

        item {
            ActionButton(
                label = "Continue",
                onClick = { onAction(IntakeAction.SubmitIntakeForm) },
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium)
                    .testTag(IntakeTestTags.CONTINUE_BUTTON)
            )
        }
    }
}

@PreviewLightDark
@Composable
fun IntakeScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            IntakeScreen(
                state = IntakeState(), onAction = { }, modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
