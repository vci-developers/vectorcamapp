package com.vci.vectorcamapp.surveillance_form.presentation

import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.surveillance_form.presentation.model.SurveillanceFormErrors

data class SurveillanceFormState(
    val isLoading: Boolean = false,
    val surveillanceForm: SurveillanceForm = SurveillanceForm(
        country = "",
        district = "",
        healthCenter = "",
        sentinelSite = "",
        householdNumber = "",
        latitude = 0F,
        longitude = 0F,
        collectionDate = System.currentTimeMillis(),
        collectionMethod = "",
        collectorName = "",
        collectorTitle = "",
        numPeopleSleptInHouse = 0,
        wasIrsConducted = false,
        monthsSinceIrs = null,
        numLlinsAvailable = 0,
        llinType = null,
        llinBrand = null,
        numPeopleSleptUnderLlin = null,
        notes = ""
    ),
    val surveillanceFormErrors: SurveillanceFormErrors = SurveillanceFormErrors(
        country = null,
        district = null,
        healthCenter = null,
        sentinelSite = null,
        householdNumber = null,
        collectionDate = null,
        collectionMethod = null,
        collectorName = null,
        collectorTitle = null,
        llinType = null,
        llinBrand = null
    )
)
