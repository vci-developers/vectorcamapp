package com.vci.vectorcamapp.surveillance_form.presentation

sealed interface SurveillanceFormAction {
    data object StartImaging: SurveillanceFormAction
}
