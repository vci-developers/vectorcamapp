package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.SessionEntity
import com.vci.vectorcamapp.core.domain.model.Session

fun SessionEntity.toDomain(): Session {
    return Session(
        localId = localId,
        remoteId = remoteId,
        houseNumber = houseNumber,
        collectorTitle = collectorTitle,
        collectorName = collectorName,
        collectionDate = collectionDate,
        collectionMethod = collectionMethod,
        specimenCondition = specimenCondition,
        createdAt = createdAt,
        completedAt = completedAt,
        submittedAt = submittedAt,
        notes = notes,
        latitude = latitude,
        longitude = longitude,
        type = type
    )
}

fun Session.toEntity(siteId: Int): SessionEntity {
    return SessionEntity(
        localId = this.localId,
        siteId = siteId,
        remoteId = this.remoteId,
        houseNumber = this.houseNumber,
        collectorTitle = this.collectorTitle,
        collectorName = this.collectorName,
        collectionDate = this.collectionDate,
        collectionMethod = this.collectionMethod,
        specimenCondition = this.specimenCondition,
        createdAt = this.createdAt,
        completedAt = this.completedAt,
        submittedAt = this.submittedAt,
        notes = this.notes,
        latitude = this.latitude,
        longitude = this.longitude,
        type = this.type
    )
}
