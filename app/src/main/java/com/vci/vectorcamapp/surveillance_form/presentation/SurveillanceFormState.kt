package com.vci.vectorcamapp.surveillance_form.presentation

import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.surveillance_form.presentation.model.SurveillanceFormErrors
import java.util.UUID

sealed class LocationState {
    object Loading : LocationState()
    data class Success(val lat: Float, val lon: Float) : LocationState()
    data class Error(val message: String) : LocationState()
}

data class SurveillanceFormState(
    val isLoading: Boolean = false,
    val locationState: LocationState = LocationState.Loading,
    val allSitesInProgram: List<Site> = emptyList(),
    val selectedDistrict: String = "",
    val selectedSentinelSite: String = "",
    val session: Session = Session(
        localId = UUID.randomUUID(),
        remoteId = null,
        houseNumber = "",
        collectorTitle = "",
        collectorName = "",
        collectionDate = System.currentTimeMillis(),
        collectionMethod = "",
        specimenCondition = "",
        createdAt = System.currentTimeMillis(),
        completedAt = null,
        submittedAt = null,
        notes = ""
    ),
    val surveillanceForm: SurveillanceForm = SurveillanceForm(
        numPeopleSleptInHouse = 0,
        wasIrsConducted = false,
        monthsSinceIrs = null,
        numLlinsAvailable = 0,
        llinType = null,
        llinBrand = null,
        numPeopleSleptUnderLlin = null,
    ),
    val surveillanceFormErrors: SurveillanceFormErrors = SurveillanceFormErrors(
        collectorTitle = null,
        collectorName = null,
        district = null,
        sentinelSite = null,
        houseNumber = null,
        llinType = null,
        llinBrand = null,
        collectionDate = null,
        collectionMethod = null,
        specimenCondition = null,
    )
)
