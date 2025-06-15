package com.vci.vectorcamapp.surveillance_form.presentation

import com.vci.vectorcamapp.surveillance_form.domain.enums.LlinBrandOption
import com.vci.vectorcamapp.surveillance_form.domain.enums.LlinTypeOption

sealed interface SurveillanceFormAction {
    data object SaveSessionProgress: SurveillanceFormAction
    data object SubmitSurveillanceForm: SurveillanceFormAction
    data class EnterNumPeopleSleptInHouse(val count: String) : SurveillanceFormAction
    data class ToggleIrsConducted(val isChecked : Boolean) : SurveillanceFormAction
    data class EnterMonthsSinceIrs(val count: String) : SurveillanceFormAction
    data class EnterNumLlinsAvailable(val count: String) : SurveillanceFormAction
    data class SelectLlinType(val option: LlinTypeOption) : SurveillanceFormAction
    data class SelectLlinBrand(val option: LlinBrandOption) : SurveillanceFormAction
    data class EnterNumPeopleSleptUnderLlin(val count: String) : SurveillanceFormAction
}
