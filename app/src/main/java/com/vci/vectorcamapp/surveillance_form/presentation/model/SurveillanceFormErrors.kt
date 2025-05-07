package com.vci.vectorcamapp.surveillance_form.presentation.model

import com.vci.vectorcamapp.surveillance_form.domain.util.FormValidationError

data class SurveillanceFormErrors(
    val country: FormValidationError?,
    val district: FormValidationError?,
    val healthCenter: FormValidationError?,
    val sentinelSite: FormValidationError?,
    val householdNumber: FormValidationError?,
    val collectionDate: FormValidationError?,
    val collectionMethod: FormValidationError?,
    val collectorName: FormValidationError?,
    val collectorTitle: FormValidationError?,
    val llinType: FormValidationError?,
    val llinBrand: FormValidationError?,
)
