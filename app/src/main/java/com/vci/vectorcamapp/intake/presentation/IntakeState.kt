package com.vci.vectorcamapp.intake.presentation

import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.intake.presentation.model.IntakeErrors
import com.vci.vectorcamapp.intake.domain.util.IntakeError

import java.util.UUID

data class IntakeState(
    val isLoading: Boolean = false,
    val allCollectors: List<Collector> = emptyList(),
    val isCurrentCollectorMissing: Boolean = false,
    val locationError: IntakeError? = null,
    val allSitesInProgram: List<Site> = emptyList(),
    val selectedDistrict: String = "",
    val selectedVillageName: String = "",
    val selectedHouseNumber: String = "",
    val session: Session = Session(
        localId = UUID.randomUUID(),
        remoteId = null,
        collectorTitle = "",
        collectorName = "",
        collectionDate = System.currentTimeMillis(),
        collectionMethod = "",
        specimenCondition = "",
        createdAt = System.currentTimeMillis(),
        completedAt = null,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        type = SessionType.SURVEILLANCE,
        collectorLastTrainedOn = null
    ),
    val surveillanceForm: SurveillanceForm? = null,
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
    ),
    val isCollectionMethodTooltipVisible: Boolean = false,
)
