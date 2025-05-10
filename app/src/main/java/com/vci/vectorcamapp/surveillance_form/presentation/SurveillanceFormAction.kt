package com.vci.vectorcamapp.surveillance_form.presentation

import com.vci.vectorcamapp.surveillance_form.domain.enums.CollectionMethodOption
import com.vci.vectorcamapp.surveillance_form.domain.enums.LlinBrandOption
import com.vci.vectorcamapp.surveillance_form.domain.enums.LlinTypeOption

sealed interface SurveillanceFormAction {
    data object SaveSessionProgress: SurveillanceFormAction
    data object SubmitSurveillanceForm: SurveillanceFormAction
    data class EnterCountry(val text: String) : SurveillanceFormAction
    data class EnterDistrict(val text: String) : SurveillanceFormAction
    data class EnterHealthCenter(val text: String) : SurveillanceFormAction
    data class EnterSentinelSite(val text: String) : SurveillanceFormAction
    data class EnterHouseholdNumber(val text: String) : SurveillanceFormAction
    data class PickCollectionDate(val date: Long) : SurveillanceFormAction
    data class SelectCollectionMethod(val option: CollectionMethodOption) : SurveillanceFormAction
    data class EnterCollectorName(val text: String) : SurveillanceFormAction
    data class EnterCollectorTitle(val text: String) : SurveillanceFormAction
    data class EnterNumPeopleSleptInHouse(val count: String) : SurveillanceFormAction
    data class ToggleIrsConducted(val isChecked : Boolean) : SurveillanceFormAction
    data class EnterMonthsSinceIrs(val count: String) : SurveillanceFormAction
    data class EnterNumLlinsAvailable(val count: String) : SurveillanceFormAction
    data class SelectLlinType(val option: LlinTypeOption) : SurveillanceFormAction
    data class SelectLlinBrand(val option: LlinBrandOption) : SurveillanceFormAction
    data class EnterNumPeopleSleptUnderLlin(val count: String) : SurveillanceFormAction
    data class EnterNotes(val text: String) : SurveillanceFormAction
}
