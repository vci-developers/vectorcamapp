package com.vci.vectorcamapp.intake.presentation

import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions.LlinBrandOption
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions.LlinTypeOption

sealed interface IntakeAction {
    data object ReturnToLandingScreen: IntakeAction
    data object SubmitIntakeForm: IntakeAction
    data class EnterCollectorTitle(val text: String) : IntakeAction
    data class EnterCollectorName(val text: String) : IntakeAction
    data class SelectDistrict(val district: String) : IntakeAction
    data class SelectVillageName(val villageName: String) : IntakeAction
    data class SelectHouseNumber(val houseNumber: String) : IntakeAction
    data class EnterNumPeopleSleptInHouse(val count: String) : IntakeAction
    data class ToggleIrsConducted(val isChecked : Boolean) : IntakeAction
    data class EnterMonthsSinceIrs(val count: String) : IntakeAction
    data class EnterNumLlinsAvailable(val count: String) : IntakeAction
    data class SelectLlinType(val option: LlinTypeOption) : IntakeAction
    data class SelectLlinBrand(val option: LlinBrandOption) : IntakeAction
    data class EnterNumPeopleSleptUnderLlin(val count: String) : IntakeAction
    data class PickCollectionDate(val date: Long) : IntakeAction
    data class UpdateCollectionMethod(val collectionMethod: String) : IntakeAction
    data class UpdateSpecimenCondition(val specimenCondition: String) : IntakeAction
    data class EnterNotes(val text: String) : IntakeAction
    data object RetryLocation: IntakeAction
    data class SetCollectionMethodInfoVisibility(val isVisible: Boolean) : IntakeAction
}
