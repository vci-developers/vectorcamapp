package com.vci.vectorcamapp.intake.presentation

import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.LocationType
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.intake.domain.util.IntakeError
import com.vci.vectorcamapp.intake.presentation.model.IntakeErrors
import java.util.UUID

data class IntakeState(
    val isLoading: Boolean = false,
    val canUpdateLocation: Boolean = false,
    val isResumingSession: Boolean = false,
    val isCheckingLocation: Boolean = false,
    val distanceToSiteMeters: Float? = null,
    val isWithinSiteRadius: Boolean = false,
    val allCollectors: List<Collector> = emptyList(),
    val isCurrentCollectorMissing: Boolean = false,
    val isCollectionMethodLocked: Boolean = false,
    val locationError: IntakeError? = null,
    val siteSelectionsByLocationTypeId: Map<Int, String> = emptyMap(),
    val allLocationTypesInProgram: List<LocationType> = emptyList(),
    val allSitesInProgram: List<Site> = emptyList(),
    val selectedSiteWithCoordinates: Site? = null,
    val selectedDistrict: String = "",
    val selectedVillageName: String = "",
    val selectedHouseNumber: String = "",
    val session: Session = Session(
        localId = UUID.randomUUID(),
        remoteId = null,
        hardwareId = null,
        collectorTitle = "",
        collectorName = "",
        collectorLastTrainedOn = 0L,
        collectionDate = System.currentTimeMillis(),
        collectionMethod = "",
        specimenCondition = "",
        createdAt = System.currentTimeMillis(),
        completedAt = null,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        gpsAccuracy = null,
        type = SessionType.SURVEILLANCE,
    ),
    val surveillanceForm: SurveillanceForm? = null,
    val form: Form? = null,
    val formQuestions: List<FormQuestion> = emptyList(),
    val formAnswersByQuestionId: Map<Int, FormAnswer> = emptyMap(),
    val intakeErrors: IntakeErrors = IntakeErrors(
        collector = null,
        district = null,
        villageName = null,
        houseNumber = null,
        llinType = null,
        llinBrand = null,
        collectionDate = null,
        collectionMethod = null,
        specimenCondition = null,
        monthsSinceIrs = null,
        numLlinsAvailable = null,
        numPeopleSleptUnderLlin = null,
        numPeopleSleptInHouse = null,
        numChildrenUnder5 = null,
        locationTypeSiteSelections = emptyMap(),
        formAnswerErrors = emptyMap(),
    ),
) {
    companion object {
        // TODO: Replace this demo-only ID with the stable question key when mobile supports it.
        private const val HOUSEHOLD_HEAD_CONSENT_QUESTION_ID = 1956
    }
    val hasHouseholdHeadConsent: Boolean
        get() = formQuestions.any {
            it.id == HOUSEHOLD_HEAD_CONSENT_QUESTION_ID && it.type == "boolean"
        } && formAnswersByQuestionId[HOUSEHOLD_HEAD_CONSENT_QUESTION_ID]?.value ==
            FormAnswerValue.StringValue("true")
    val canFillForm: Boolean
        get() = !isLoading && (isResumingSession ||
            (!isCheckingLocation && locationError == null && isWithinSiteRadius))
}
