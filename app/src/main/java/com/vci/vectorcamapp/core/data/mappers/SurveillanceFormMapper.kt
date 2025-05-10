package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.SurveillanceFormEntity
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import java.util.UUID

fun SurveillanceFormEntity.toDomain() : SurveillanceForm {
    return SurveillanceForm(
        country = this.country,
        district = this.district,
        healthCenter = this.healthCenter,
        sentinelSite = this.sentinelSite,
        householdNumber = this.householdNumber,
        latitude = this.latitude,
        longitude = this.longitude,
        collectionDate = this.collectionDate,
        collectionMethod = this.collectionMethod,
        collectorName = this.collectorName,
        collectorTitle = this.collectorTitle,
        numPeopleSleptInHouse = this.numPeopleSleptInHouse,
        wasIrsConducted = this.wasIrsConducted,
        monthsSinceIrs = this.monthsSinceIrs,
        numLlinsAvailable = this.numLlinsAvailable,
        llinType = this.llinType,
        llinBrand = this.llinBrand,
        numPeopleSleptUnderLlin = this.numPeopleSleptUnderLlin,
        notes = this.notes
    )
}

fun SurveillanceForm.toEntity(sessionId: UUID) : SurveillanceFormEntity {
    return SurveillanceFormEntity(
        sessionId = sessionId,
        country = this.country,
        district = this.district,
        healthCenter = this.healthCenter,
        sentinelSite = this.sentinelSite,
        householdNumber = this.householdNumber,
        latitude = this.latitude,
        longitude = this.longitude,
        collectionDate = this.collectionDate,
        collectionMethod = this.collectionMethod,
        collectorName = this.collectorName,
        collectorTitle = this.collectorTitle,
        numPeopleSleptInHouse = this.numPeopleSleptInHouse,
        wasIrsConducted = this.wasIrsConducted,
        monthsSinceIrs = this.monthsSinceIrs,
        numLlinsAvailable = this.numLlinsAvailable,
        llinType = this.llinType,
        llinBrand = this.llinBrand,
        numPeopleSleptUnderLlin = this.numPeopleSleptUnderLlin,
        notes = this.notes,
    )
}
