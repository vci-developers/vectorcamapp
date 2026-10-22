package com.vci.vectorcamapp.intake.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.camera.BarcodeScannerDialog
import com.vci.vectorcamapp.core.presentation.components.form.DatePickerField
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.form.ToggleField
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.core.presentation.extensions.displayText
import com.vci.vectorcamapp.core.presentation.util.error.toString
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.domain.util.FormQuestionPrerequisiteEvaluator
import com.vci.vectorcamapp.intake.presentation.components.DynamicFormField
import com.vci.vectorcamapp.intake.presentation.components.IntakeTile
import com.vci.vectorcamapp.intake.presentation.components.PracticeSessionWarningBanner
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import java.util.UUID

@Composable
fun IntakeScreen(
    state: IntakeState, onAction: (IntakeAction) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBarcodeScannerVisible by remember { mutableStateOf(false) }

    BackHandler {
        onAction(IntakeAction.ReturnToPreviousScreen)
    }

    ScreenHeader(
        title = stringResource(R.string.intake_title_screen, state.session.type.displayText(context)),
        subtitle = stringResource(R.string.intake_body_subtitle),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.intake_content_description_back),
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable {
                        onAction(IntakeAction.ReturnToPreviousScreen)
                    }
            )
        },
        modifier = modifier
    ) {
        if (state.session.type == SessionType.PRACTICE) {
            item {
                PracticeSessionWarningBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.dimensions.paddingMedium)
                        .padding(vertical = MaterialTheme.dimensions.paddingSmall)
                )
            }
        }

        item {
            IntakeTile(
                title = stringResource(R.string.intake_title_general_info),
                iconPainter = painterResource(R.drawable.ic_info),
                iconDescription = stringResource(R.string.intake_content_description_general_info)
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
                    label = stringResource(R.string.intake_label_collector),
                    required = true,
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
                                    verticalArrangement = Arrangement.spacedBy(
                                        MaterialTheme.dimensions.spacingExtraSmall
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(
                                            MaterialTheme.dimensions.spacingExtraSmall
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_warning),
                                            contentDescription = stringResource(
                                                R.string.intake_content_description_missing_collector
                                            ),
                                            tint = MaterialTheme.colors.error,
                                            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium)
                                        )
                                        Text(
                                            text = stringResource(R.string.intake_title_collector_not_found),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colors.error
                                        )
                                    }

                                    Text(
                                        text = stringResource(R.string.intake_body_collector_not_found),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colors.textPrimary.copy(alpha = 0.8f)
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(
                                            MaterialTheme.dimensions.spacingExtraExtraSmall
                                        )
                                    ) {
                                        Text(
                                            text = stringResource(R.string.intake_label_name),
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
                                        verticalArrangement = Arrangement.spacedBy(
                                            MaterialTheme.dimensions.spacingExtraExtraSmall
                                        )
                                    ) {
                                        Text(
                                            text = stringResource(R.string.intake_label_title),
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
                                    Text(text = stringResource(R.string.intake_action_register_collector))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TextEntryField(
                            label = stringResource(R.string.intake_label_hardware_id),
                            value = state.session.hardwareId.orEmpty(),
                            onValueChange = { onAction(IntakeAction.EnterHardwareId(it)) },
                            singleLine = true,
                        )
                    }
                    IconButton(onClick = { isBarcodeScannerVisible = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = stringResource(
                                R.string.intake_content_description_scan_hardware_id
                            ),
                            tint = MaterialTheme.colors.icon,
                        )
                    }
                }

                DatePickerField(
                    label = stringResource(R.string.intake_label_collection_date),
                    required = true,
                    selectedDateInMillis = state.session.collectionDate,
                    onDateSelected = { onAction(IntakeAction.PickCollectionDate(it)) },
                    error = state.intakeErrors.collectionDate,
                    modifier = Modifier.fillMaxWidth()
                )

                val isOtherCollectionMethod = state.session.collectionMethod.startsWith(
                    IntakeDropdownOptions.CollectionMethodOption.OTHER.label,
                    ignoreCase = true
                )

                DropdownField(
                    label = stringResource(R.string.intake_label_collection_method),
                    required = true,
                    enabled = !state.isCollectionMethodLocked,
                    options = IntakeDropdownOptions.CollectionMethodOption.entries,
                    selectedOption = if (isOtherCollectionMethod) {
                        IntakeDropdownOptions.CollectionMethodOption.OTHER
                    } else {
                        IntakeDropdownOptions.CollectionMethodOption.entries.firstOrNull { it.label == state.session.collectionMethod }
                    },
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

                if (state.isCollectionMethodLocked) {
                    Text(
                        text = stringResource(R.string.intake_body_method_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.warning,
                        modifier = Modifier.fillMaxWidth()
                    )
                }


                if (isOtherCollectionMethod) {
                    TextEntryField(
                        label = stringResource(R.string.intake_label_other_collection_method),
                        required = true,
                        value = state.session.collectionMethod.removePrefix(
                            IntakeDropdownOptions.CollectionMethodOption.OTHER.label
                        )
                            .trimStart(),
                        onValueChange = {
                            onAction(
                                IntakeAction.UpdateCollectionMethod(
                                    "${IntakeDropdownOptions.CollectionMethodOption.OTHER.label} $it"
                                )
                            )
                        },
                        singleLine = true,
                        error = state.intakeErrors.collectionMethod
                    )
                }

                val isOtherSpecimenCondition = state.session.specimenCondition.startsWith(
                    IntakeDropdownOptions.SpecimenConditionOption.OTHER.label,
                    ignoreCase = true
                )

                DropdownField(
                    label = stringResource(R.string.intake_label_specimen_condition),
                    required = true,
                    options = IntakeDropdownOptions.SpecimenConditionOption.entries,
                    selectedOption = if (isOtherSpecimenCondition) {
                        IntakeDropdownOptions.SpecimenConditionOption.OTHER
                    } else {
                        IntakeDropdownOptions.SpecimenConditionOption.entries.firstOrNull { it.label == state.session.specimenCondition }
                    },
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
                        label = stringResource(R.string.intake_label_other_specimen_condition),
                        required = true,
                        value = state.session.specimenCondition.removePrefix(
                            IntakeDropdownOptions.SpecimenConditionOption.OTHER.label
                        )
                            .trimStart(),
                        onValueChange = {
                            onAction(
                                IntakeAction.UpdateSpecimenCondition(
                                    "${IntakeDropdownOptions.SpecimenConditionOption.OTHER.label} $it"
                                )
                            )
                        },
                        singleLine = true,
                        error = state.intakeErrors.specimenCondition
                    )
                }
            }
        }

        item {
            IntakeTile(
                title = stringResource(R.string.intake_title_geographical_info),
                iconPainter = painterResource(id = R.drawable.ic_pin),
                iconDescription = stringResource(R.string.intake_content_description_geographical_info)
            ) {
                if (state.allSitesInProgram.any { !it.district.isNullOrBlank() }) {
                    DropdownField(
                        label = stringResource(R.string.intake_label_district),
                        enabled = !state.isResumingSession,
                        required = true,
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
                            label = stringResource(R.string.intake_label_village_name),
                            enabled = !state.isResumingSession,
                            required = true,
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
                            label = stringResource(R.string.intake_label_house_number),
                            enabled = !state.isResumingSession,
                            required = true,
                            options = state.allSitesInProgram.filter {
                                it.district == state.selectedDistrict && it.villageName == state.selectedVillageName
                            }
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
                                enabled = !state.isResumingSession,
                                required = true,
                                options = availableOptions,
                                selectedOption = state.siteSelectionsByLocationTypeId[locationType.id],
                                onOptionSelected = {
                                    onAction(
                                        IntakeAction.SelectLocationTypeSiteOption(
                                            locationType.id,
                                            it
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

                val site = state.selectedSiteWithCoordinates
                val distanceToSiteMeters = state.distanceToSiteMeters
                if (state.canUpdateLocation) {
                    ActionButton(
                        label = stringResource(R.string.intake_action_update_location),
                        onClick = { onAction(IntakeAction.RetryLocation) },
                        enabled = !state.isLoading && !state.isCheckingLocation,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val locations = listOfNotNull(
                    Triple(R.string.intake_label_collection_location, state.session.latitude, state.session.longitude),
                    site?.takeIf { it.latitude != null && it.longitude != null }?.let {
                        Triple(R.string.intake_label_site_location, it.latitude, it.longitude)
                    }
                )
                locations.forEachIndexed { index, (title, latitude, longitude) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(MaterialTheme.dimensions.paddingMedium),
                        color = MaterialTheme.colors.appBackground,
                        border = BorderStroke(
                            MaterialTheme.dimensions.dividerThicknessThin,
                            MaterialTheme.colors.fieldBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                        ) {
                            Text(
                                text = stringResource(title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colors.textPrimary
                            )
                            when {
                                latitude != null && longitude != null -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(
                                            MaterialTheme.dimensions.spacingExtraSmall
                                        )
                                    ) {
                                        listOf(
                                            R.string.intake_label_latitude to latitude,
                                            R.string.intake_label_longitude to longitude
                                        ).forEach { (label, value) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = stringResource(label),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colors.textSecondary
                                                )
                                                Text(
                                                    text = value.toString(),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colors.textPrimary
                                                )
                                            }
                                        }
                                        state.session.gpsAccuracy?.takeIf { index == 0 }?.let { gpsAccuracy ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.intake_label_gps_accuracy),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colors.textSecondary
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.intake_value_gps_accuracy,
                                                        gpsAccuracy
                                                    ),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colors.textPrimary
                                                )
                                            }
                                        }
                                        distanceToSiteMeters?.takeIf { index == 1 }?.let { distance ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.intake_label_distance_to_site),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colors.textSecondary
                                                )
                                                Text(
                                                    text = if (distance < 1000f) {
                                                        stringResource(
                                                            R.string.intake_label_distance_to_site_meters,
                                                            distance
                                                        )
                                                    } else {
                                                        stringResource(
                                                            R.string.intake_label_distance_to_site_kilometers,
                                                            distance / 1000f
                                                        )
                                                    },
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colors.textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                                !state.canUpdateLocation && !state.isLoading -> {
                                    Text(
                                        text = stringResource(R.string.intake_body_collection_location_not_recorded),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colors.textSecondary
                                    )
                                }
                                state.locationError != null -> Unit
                                else -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(
                                            MaterialTheme.dimensions.spacingMedium
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colors.secondary,
                                            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge)
                                        )
                                        Text(
                                            stringResource(R.string.intake_body_getting_location),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.canFillForm && state.form != null) {
            item {
                IntakeTile(
                    title = state.form.name,
                    iconPainter = painterResource(id = R.drawable.ic_clipboard),
                    iconDescription = stringResource(R.string.intake_content_description_surveillance_form)
                ) {
                    val answerMap = state.formAnswersByQuestionId.mapValues { (_, answer) -> answer.value }

                    state.formQuestions.forEach { question ->
                        if (FormQuestionPrerequisiteEvaluator.evaluate(
                                question.prerequisite,
                                answerMap
                            )
                        ) {
                            DynamicFormField(
                                question = question,
                                    value = state.formAnswersByQuestionId.getValue(question.id).value,
                                error = state.intakeErrors.formAnswerErrors[question.id],
                                onValueChange = {
                                    onAction(IntakeAction.UpdateFormAnswer(question.id, it))
                                }
                            )
                        }
                    }
                }
            }
        } else if (state.canFillForm) {
            state.surveillanceForm?.let { surveillanceForm ->
                item {
                    IntakeTile(
                        title = stringResource(R.string.intake_title_surveillance_form),
                        iconPainter = painterResource(id = R.drawable.ic_clipboard),
                        iconDescription = stringResource(R.string.intake_content_description_surveillance_form)
                    ) {
                        TextEntryField(
                            label = stringResource(R.string.intake_label_num_people_house),
                            required = true,
                            value = if (surveillanceForm.numPeopleSleptInHouse < 0) {
                                ""
                            } else {
                                surveillanceForm.numPeopleSleptInHouse.toString()
                            },
                            onValueChange = { onAction(IntakeAction.EnterNumPeopleSleptInHouse(it)) },
                            singleLine = true,
                            error = state.intakeErrors.numPeopleSleptInHouse,
                        )

                        TextEntryField(
                            label = stringResource(R.string.intake_label_num_children_under_5),
                            required = true,
                            value = surveillanceForm.numChildrenUnder5?.toString() ?: "",
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
                            onCheckedChange = {
                                onAction(
                                    IntakeAction.ToggleIrsConducted(
                                        it
                                    )
                                )
                            }
                        )

                        surveillanceForm.monthsSinceIrs?.let { monthsSinceIrs ->
                            TextEntryField(
                                label = stringResource(R.string.intake_label_months_since_irs),
                                required = true,
                                value = if (monthsSinceIrs < 0) {
                                    ""
                                } else {
                                    monthsSinceIrs.toString()
                                },
                                onValueChange = { onAction(IntakeAction.EnterMonthsSinceIrs(it)) },
                                singleLine = true,
                                error = state.intakeErrors.monthsSinceIrs,
                            )
                        }

                        TextEntryField(
                            label = stringResource(R.string.intake_label_num_llins),
                            required = true,
                            value = if (surveillanceForm.numLlinsAvailable < 0) {
                                ""
                            } else {
                                surveillanceForm.numLlinsAvailable.toString()
                            },
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
                                label = stringResource(R.string.intake_label_llin_brand),
                                required = true,
                                options = IntakeDropdownOptions.LlinBrandOption.entries.filter {
                                    it.type?.label == surveillanceForm.llinType || it.type == null
                                },
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
                                label = stringResource(R.string.intake_label_num_people_llin),
                                required = true,
                                value = if (numPeopleSleptUnderLlin < 0) {
                                    ""
                                } else {
                                    numPeopleSleptUnderLlin.toString()
                                },
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
                title = stringResource(R.string.intake_title_additional_notes),
                iconPainter = painterResource(id = R.drawable.ic_notes),
                iconDescription = stringResource(R.string.intake_content_description_additional_notes)
            ) {
                TextEntryField(
                    label = stringResource(R.string.intake_label_notes),
                    value = state.session.notes,
                    onValueChange = { onAction(IntakeAction.EnterNotes(it)) },
                    placeholder = stringResource(R.string.intake_placeholder_notes),
                    maxCharacters = 1000,
                )
            }
        }

        if (state.session.type == SessionType.PRACTICE) {
            item {
                PracticeSessionWarningBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.dimensions.paddingMedium)
                        .padding(top = MaterialTheme.dimensions.paddingSmall)
                )
            }
        }

        item {
            val gateMessage = when {
                state.isLoading -> null
                state.isCheckingLocation -> R.string.intake_body_getting_location
                !state.isResumingSession && state.locationError != null -> R.string.intake_body_location_error
                !state.isResumingSession && state.distanceToSiteMeters != null && !state.isWithinSiteRadius ->
                    R.string.intake_body_outside_site_radius
                !state.canFillForm -> R.string.intake_error_site_proximity_required
                !state.hasHouseholdHeadConsent -> R.string.intake_error_consent_required
                else -> null
            }
            if (gateMessage != null) {
                Text(
                    text = when (gateMessage) {
                        R.string.intake_body_location_error -> stringResource(
                            gateMessage, requireNotNull(state.locationError).toString(context)
                        )
                        R.string.intake_body_outside_site_radius -> stringResource(
                            gateMessage, requireNotNull(state.distanceToSiteMeters) / 1000
                        )
                        else -> stringResource(gateMessage)
                    },
                    color = if (state.isCheckingLocation) MaterialTheme.colors.textSecondary
                        else MaterialTheme.colors.error,
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimensions.paddingMedium)
                )
            }
            ActionButton(
                label = stringResource(R.string.intake_action_begin_imaging, state.session.type.displayText(context)),
                onClick = { onAction(IntakeAction.SubmitIntakeForm) },
                enabled = state.canFillForm && state.hasHouseholdHeadConsent,
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium)
            )
        }
    }

    if (isBarcodeScannerVisible) {
        BarcodeScannerDialog(
            title = stringResource(R.string.intake_title_scan_hardware_id),
            description = stringResource(R.string.intake_body_scan_hardware_id),
            onBarcodeScanned = { hardwareId ->
                onAction(IntakeAction.EnterHardwareId(hardwareId))
                isBarcodeScannerVisible = false
            },
            onDismiss = { isBarcodeScannerVisible = false },
            transformValue = { value ->
                value.trim().takeIf { trimmed ->
                    trimmed.isNotEmpty() && trimmed.length <= MAX_HARDWARE_ID_LENGTH &&
                        trimmed.all { it.code <= MAX_HARDWARE_ID_CHARACTER_CODE || it.isWhitespace() }
                }
            },
        )
    }
}

private const val MAX_HARDWARE_ID_LENGTH = 200
private const val MAX_HARDWARE_ID_CHARACTER_CODE = 255

@PreviewLightDark
@Composable
fun SurveillanceFormScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            IntakeScreen(
                state = IntakeState(),
                onAction = { },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
