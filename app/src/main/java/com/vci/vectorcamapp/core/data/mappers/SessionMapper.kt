package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.SessionEntity
import com.vci.vectorcamapp.core.domain.model.Session

fun SessionEntity.toDomain(): Session {
    return Session(
        localId = this.localId,
        remoteId = this.remoteId,
        hardwareId = this.hardwareId,
        collectorTitle = this.collectorTitle,
        collectorName = this.collectorName,
        collectorLastTrainedOn = this.collectorLastTrainedOn,
        collectionDate = this.collectionDate,
        collectionMethod = this.collectionMethod,
        specimenCondition = this.specimenCondition,
        createdAt = this.createdAt,
        completedAt = this.completedAt,
        submittedAt = this.submittedAt,
        notes = this.notes,
        latitude = this.latitude,
        longitude = this.longitude,
        type = this.type,
    )
}

fun Session.toEntity(siteId: Int): SessionEntity {
    return SessionEntity(
        localId = this.localId,
        siteId = siteId,
        remoteId = this.remoteId,
        hardwareId = this.hardwareId,
        collectorTitle = this.collectorTitle,
        collectorName = this.collectorName,
        collectorLastTrainedOn = this.collectorLastTrainedOn,
        collectionDate = this.collectionDate,
        collectionMethod = this.collectionMethod,
        specimenCondition = this.specimenCondition,
        createdAt = this.createdAt,
        completedAt = this.completedAt,
        submittedAt = this.submittedAt,
        notes = this.notes,
        latitude = this.latitude,
        longitude = this.longitude,
        type = this.type,
    )
}
