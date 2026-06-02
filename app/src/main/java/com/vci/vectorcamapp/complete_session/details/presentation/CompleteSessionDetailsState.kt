package com.vci.vectorcamapp.complete_session.details.presentation

import com.vci.vectorcamapp.complete_session.details.presentation.enums.CompleteSessionDetailsTab
import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.composites.FormAnswerAndQuestion
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import java.util.UUID

data class CompleteSessionDetailsState(
    val selectedTab: CompleteSessionDetailsTab = CompleteSessionDetailsTab.SESSION_FORM,
    val session: Session = Session(
        localId = UUID(0, 0),
        remoteId = null,
        hardwareId = null,
        collectorTitle = "",
        collectorName = "",
        collectorLastTrainedOn = 0L,
        collectionDate = 0L,
        collectionMethod = "",
        specimenCondition = "",
        createdAt = 0L,
        completedAt = null,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        type = SessionType.SURVEILLANCE,
    ),
    val site: Site = Site(
        id = -1,
        district = "",
        subCounty = "",
        parish = "",
        villageName = "",
        houseNumber = "",
        healthCenter = "",
        isActive = true,
        name = "",
        locationHierarchy = emptyMap()
    ),
    val surveillanceForm: SurveillanceForm? = null,
    val form: Form? = null,
    val sessionScopedFormAnswersAndQuestions: List<FormAnswerAndQuestion> = emptyList(),
    val sessionUnits: List<SessionUnit> = emptyList(),
    val sessionUnitAnswersAndQuestionsByUnitId: Map<UUID, List<FormAnswerAndQuestion>> = emptyMap(),
    val bucketNameBySessionUnitId: Map<UUID, String> = emptyMap(),
    val sessionUnitIdBySpecimenId: Map<String, UUID?> = emptyMap(),
    val specimensWithImagesAndInferenceResults: List<SpecimenWithSpecimenImagesAndInferenceResults> = emptyList(),
    val searchQuery: String = "",
    val isSearchTooltipVisible: Boolean = false
)
