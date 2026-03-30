package com.vci.vectorcamapp.intake.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.form.DatePickerField
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.form.ToggleField
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.core.presentation.components.pill.InfoPill
import com.vci.vectorcamapp.core.presentation.components.tooltip.Tooltip
import com.vci.vectorcamapp.core.presentation.extensions.displayText
import com.vci.vectorcamapp.core.presentation.util.error.toString
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.domain.util.IntakeError
import com.vci.vectorcamapp.intake.presentation.components.CollectionMethodTooltipRow
import com.vci.vectorcamapp.intake.presentation.components.DynamicFormField
import com.vci.vectorcamapp.intake.presentation.components.IntakeTile
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import java.util.UUID

@Composable
fun IntakeScreen(
    state: IntakeState, onAction: (IntakeAction) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BackHandler {
        onAction(IntakeAction.ReturnToPreviousScreen)
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
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable {
                        onAction(IntakeAction.ReturnToPreviousScreen)
                    })
        },
        modifier = modifier
    ) {
        item {
            IntakeTile(
                title = "General Information",
                iconPainter = painterResource(R.drawable.ic_info),
                iconDescription = "General Information Icon"
            ) {
                val selectedCollector = if (state.isCurrentCollectorMissing) {
                    Collector(
                        id = UUID.randomUUID(),
                        name = state.session.collectorName,
                        title = state.session.collectorTitle,
                        lastTrainedOn = state.session.collectorLastTrainedOn
                    )
                } else {
                    state.allCollectors.firstOrNull { collector ->
                        collector.name == state.session.collectorName && collector.title == state.session.collectorTitle
                    }
                }

                DropdownField(
                    label = "Collector",
                    options = state.allCollectors,
                    selectedOption = selectedCollector,
                    onOptionSelected = { selected: Collector ->
                        onAction(IntakeAction.SelectCollector(selected))
                    },
                    error = state.intakeErrors.collector,
                    modifier = Modifier.fillMaxWidth()
                ) { collector ->
                    Text(
                        text = collector.name + ", " + collector.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                if (state.isCurrentCollectorMissing) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MaterialTheme.dimensions.paddingSmall),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colors.appBackground
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = MaterialTheme.dimensions.paddingLarge,
                                    bottom = MaterialTheme.dimensions.paddingMedium
                                )
                                .padding(horizontal = MaterialTheme.dimensions.paddingMedium)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_warning),
                                            contentDescription = "Missing collector",
                                            tint = MaterialTheme.colors.error,
                                            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium)
                                        )
                                        Text(
                                            text = "Collector not found",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colors.error
                                        )
                                    }

                                    Text(
                                        text = "The collector associated with this session isn’t in your current list. You can select an existing collector or register this one.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colors.textPrimary.copy(alpha = 0.8f)
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraExtraSmall)
                                    ) {
                                        Text(
                                            text = "Name",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colors.textSecondary
                                        )
                                        Text(
                                            text = state.session.collectorName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colors.textPrimary
                                        )
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraExtraSmall)
                                    ) {
                                        Text(
                                            text = "Title",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colors.textSecondary
                                        )
                                        Text(
                                            text = state.session.collectorTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colors.textPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier.height(MaterialTheme.dimensions.spacingSmall))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = MaterialTheme.dimensions.paddingSmall),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { onAction(IntakeAction.RegisterMissingCollector) },
                                    shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colors.primary,
                                        contentColor = MaterialTheme.colors.buttonText
                                    )
                                ) {
                                    Text(text = "Register Missing Collector")
                                }
                            }
                        }
                    }
                }

                TextEntryField(
                    label = "Hardware ID",
                    value = state.session.hardwareId.orEmpty(),
                    onValueChange = { onAction(IntakeAction.EnterHardwareId(it)) },
                    singleLine = true,
                )

                DatePickerField(
                    label = "Collection Date",
                    selectedDateInMillis = state.session.collectionDate,
                    onDateSelected = { onAction(IntakeAction.PickCollectionDate(it)) },
                    error = state.intakeErrors.collectionDate,
                    modifier = Modifier.fillMaxWidth()
                )

                val isOtherCollectionMethod = state.session.collectionMethod.startsWith(
                    IntakeDropdownOptions.CollectionMethodOption.OTHER.label, ignoreCase = true
                )

                DropdownField(
                    label = "Collection Method",
                    options = IntakeDropdownOptions.CollectionMethodOption.entries,
                    selectedOption = if (isOtherCollectionMethod) IntakeDropdownOptions.CollectionMethodOption.OTHER
                    else IntakeDropdownOptions.CollectionMethodOption.entries.firstOrNull { it.label == state.session.collectionMethod },
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

                Tooltip(
                    isVisible = state.isCollectionMethodTooltipVisible,
                    onClick = { onAction(IntakeAction.ShowCollectionMethodTooltipDialog) },
                    onDismiss = { onAction(IntakeAction.HideCollectionMethodTooltipDialog) },
                    buttonText = "Tap to learn more about collection methods"
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
                    ) {
                        Text(
                            text = "Collection Methods",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = MaterialTheme.dimensions.paddingSmall)
                        )
                        CollectionMethodTooltipRow(
                            title = "CDC Light Trap",
                            description = "A battery powered trap that uses a light to attract and collect mosquitoes at night.",
                            iconPainter = painterResource(id = R.drawable.ic_light_trap),
                            iconDescription = "CDC Light Trap Icon",
                        )
                        CollectionMethodTooltipRow(
                            title = "Human Landing Catch",
                            description = "A person exposes part of their body and collects mosquitoes that land on the skin.",
                            iconPainter = painterResource(id = R.drawable.ic_human),
                            iconDescription = "Human Landing Catch Icon"
                        )
                        CollectionMethodTooltipRow(
                            title = "Pyrethrum Spray Catch",
                            description = "An indoor collection method that uses insecticide spray to knock down resting mosquitoes onto a sheet.",
                            iconPainter = painterResource(id = R.drawable.ic_spray),
                            iconDescription = "Pyrethrum Spray Catch"
                        )
                    }
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

                val isOtherSpecimenCondition = state.session.specimenCondition.startsWith(
                    IntakeDropdownOptions.SpecimenConditionOption.OTHER.label, ignoreCase = true
                )

                DropdownField(
                    label = "Specimen Condition",
                    options = IntakeDropdownOptions.SpecimenConditionOption.entries,
                    selectedOption = if (isOtherSpecimenCondition) IntakeDropdownOptions.SpecimenConditionOption.OTHER
                    else IntakeDropdownOptions.SpecimenConditionOption.entries.firstOrNull { it.label == state.session.specimenCondition },
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
                if (state.allSitesInProgram.any { !it.district.isNullOrBlank() }) {
                    DropdownField(
                        label = "District",
                        options = state.allSitesInProgram.mapNotNull { it.district }.distinct(),
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
                                .mapNotNull { it.villageName }.distinct(),
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
                                .mapNotNull { it.houseNumber }.distinct(),
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
                } else {
                    state.allLocationTypesInProgram.forEachIndexed { index, locationType ->
                        val parentLocationTypes = state.allLocationTypesInProgram.take(index)
                        val shouldShowDropdown =
                            index == 0 || parentLocationTypes.all { !state.siteSelectionsByLocationTypeId[it.id].isNullOrBlank() }

                        if (shouldShowDropdown) {
                            val filteredSites = state.allSitesInProgram.filter { site ->
                                val locationHierarchy =
                                    site.locationHierarchy ?: return@filter false
                                parentLocationTypes.all { parentLocationType ->
                                    val selectedParentOption =
                                        state.siteSelectionsByLocationTypeId[parentLocationType.id]
                                            ?: return@all false
                                    locationHierarchy[parentLocationType.name] == selectedParentOption
                                }
                            }

                            val availableOptions =
                                filteredSites.mapNotNull { it.locationHierarchy?.get(locationType.name) }
                                    .distinct()

                            DropdownField(
                                label = locationType.name,
                                options = availableOptions,
                                selectedOption = state.siteSelectionsByLocationTypeId[locationType.id],
                                onOptionSelected = {
                                    onAction(
                                        IntakeAction.SelectLocationTypeSiteOption(
                                            locationType.id, it
                                        )
                                    )
                                },
                                error = state.intakeErrors.locationTypeSiteSelections[locationType.id]
                            ) { locationTypeSiteOption ->
                                Text(
                                    text = locationTypeSiteOption,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colors.textPrimary
                                )
                            }
                        }
                    }
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
                                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge)
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

        if (state.form != null) {
            item {
                IntakeTile(
                    title = state.form.name,
                    iconPainter = painterResource(id = R.drawable.ic_clipboard),
                    iconDescription = "Surveillance Form Icon"
                ) {
                    state.formQuestions.forEach { question ->
                        DynamicFormField(
                            question = question,
                            value = state.formAnswers[question.id]?.value.orEmpty(),
                            error = state.intakeErrors.formAnswerErrors[question.id],
                            onValueChange = {
                                onAction(
                                    IntakeAction.UpdateFormAnswer(
                                        question.id, it
                                    )
                                )
                            })
                    }
                }
            }
        } else {
            state.surveillanceForm?.let { surveillanceForm ->
                item {
                    IntakeTile(
                        title = "Surveillance Form",
                        iconPainter = painterResource(id = R.drawable.ic_clipboard),
                        iconDescription = "Surveillance Form Icon"
                    ) {
                        TextEntryField(
                            label = "Number of People Living in the House",
                            value = if (surveillanceForm.numPeopleSleptInHouse < 0) ""
                            else surveillanceForm.numPeopleSleptInHouse.toString(),
                            onValueChange = { onAction(IntakeAction.EnterNumPeopleSleptInHouse(it)) },
                            singleLine = true,
                            error = state.intakeErrors.numPeopleSleptInHouse,
                        )

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

                        surveillanceForm.monthsSinceIrs?.let { monthsSinceIrs ->
                            TextEntryField(
                                label = "Months Since IRS",
                                value = if (monthsSinceIrs < 0) ""
                                else monthsSinceIrs.toString(),
                                onValueChange = { onAction(IntakeAction.EnterMonthsSinceIrs(it)) },
                                singleLine = true,
                                error = state.intakeErrors.monthsSinceIrs,
                            )
                        }

                        TextEntryField(
                            label = "Number of LLINs Available",
                            value = if (surveillanceForm.numLlinsAvailable < 0) ""
                            else surveillanceForm.numLlinsAvailable.toString(),
                            onValueChange = { onAction(IntakeAction.EnterNumLlinsAvailable(it)) },
                            singleLine = true,
                            error = state.intakeErrors.numLlinsAvailable,
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
                                options = IntakeDropdownOptions.LlinBrandOption.entries.filter { it.type?.label == surveillanceForm.llinType || it.type == null },
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

                        surveillanceForm.numPeopleSleptUnderLlin?.let { numPeopleSleptUnderLlin ->
                            TextEntryField(
                                label = "Number of People who Slept Under LLIN",
                                value = if (numPeopleSleptUnderLlin < 0) ""
                                else numPeopleSleptUnderLlin.toString(),
                                onValueChange = {
                                    onAction(
                                        IntakeAction.EnterNumPeopleSleptUnderLlin(
                                            it
                                        )
                                    )
                                },
                                singleLine = true,
                                error = state.intakeErrors.numPeopleSleptUnderLlin,
                            )
                        }
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
                    maxCharacters = 1000,
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
