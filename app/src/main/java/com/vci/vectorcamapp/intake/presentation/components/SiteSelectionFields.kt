package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.LocationType
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.intake.presentation.IntakeAction
import com.vci.vectorcamapp.intake.presentation.IntakeState

@Composable
internal fun SiteSelectionFields(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
) {
    if (state.allSitesInProgram.any { !it.district.isNullOrBlank() }) {
        DistrictSiteFields(state = state, onAction = onAction)
    } else {
        state.allLocationTypesInProgram.forEachIndexed { index, locationType ->
            LocationHierarchyField(
                state = state,
                locationType = locationType,
                parentLocationTypes = state.allLocationTypesInProgram.take(index),
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun DistrictSiteFields(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
) {
    DropdownField(
        label = stringResource(R.string.intake_label_district),
        required = true,
        options = state.allSitesInProgram.mapNotNull { it.district }.distinct(),
        selectedOption = state.selectedDistrict,
        onOptionSelected = { onAction(IntakeAction.SelectDistrict(it)) },
        error = state.intakeErrors.district,
    ) { district ->
        DropdownOptionLabel(text = district)
    }

    if (state.selectedDistrict.isNotBlank()) {
        DropdownField(
            label = stringResource(R.string.intake_label_village_name),
            required = true,
            options = state.allSitesInProgram
                .filter { it.district == state.selectedDistrict }
                .mapNotNull { it.villageName }
                .distinct(),
            selectedOption = state.selectedVillageName,
            onOptionSelected = { onAction(IntakeAction.SelectVillageName(it)) },
            error = state.intakeErrors.villageName,
        ) { villageName ->
            DropdownOptionLabel(text = villageName)
        }
    }

    if (state.selectedVillageName.isNotBlank()) {
        DropdownField(
            label = stringResource(R.string.intake_label_house_number),
            required = true,
            options = state.allSitesInProgram
                .filter { it.district == state.selectedDistrict && it.villageName == state.selectedVillageName }
                .mapNotNull { it.houseNumber }
                .distinct(),
            selectedOption = state.selectedHouseNumber,
            onOptionSelected = { onAction(IntakeAction.SelectHouseNumber(it)) },
            error = state.intakeErrors.houseNumber,
        ) { houseNumber ->
            DropdownOptionLabel(text = houseNumber)
        }
    }
}

@Composable
private fun LocationHierarchyField(
    state: IntakeState,
    locationType: LocationType,
    parentLocationTypes: List<LocationType>,
    onAction: (IntakeAction) -> Unit,
) {
    val shouldShowDropdown = parentLocationTypes.isEmpty() || parentLocationTypes.all {
        !state.siteSelectionsByLocationTypeId[it.id].isNullOrBlank()
    }
    if (!shouldShowDropdown) return

    DropdownField(
        label = locationType.name,
        required = true,
        options = sitesMatchingParents(
            state.allSitesInProgram,
            parentLocationTypes,
            state.siteSelectionsByLocationTypeId
        )
            .mapNotNull { it.locationHierarchy?.get(locationType.name) }
            .distinct(),
        selectedOption = state.siteSelectionsByLocationTypeId[locationType.id],
        onOptionSelected = {
            onAction(IntakeAction.SelectLocationTypeSiteOption(locationType.id, it))
        },
        error = state.intakeErrors.locationTypeSiteSelections[locationType.id],
    ) { option ->
        DropdownOptionLabel(text = option)
    }
}

private fun sitesMatchingParents(
    sites: List<Site>,
    parentLocationTypes: List<LocationType>,
    selections: Map<Int, String>,
): List<Site> {
    return sites.filter { site ->
        val locationHierarchy = site.locationHierarchy ?: return@filter false
        parentLocationTypes.all { parent ->
            val selectedParentOption = selections[parent.id] ?: return@all false
            locationHierarchy[parent.name] == selectedParentOption
        }
    }
}
