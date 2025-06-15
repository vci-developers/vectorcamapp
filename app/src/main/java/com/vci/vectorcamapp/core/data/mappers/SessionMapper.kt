package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.dto.SessionDto
import com.vci.vectorcamapp.core.data.room.entities.SessionEntity
import com.vci.vectorcamapp.core.domain.model.Session
import java.util.UUID

fun SessionEntity.toDomain(): Session {
    return Session(
        localId = this.localId,
        remoteId =  this.remoteId,
        houseNumber = this.houseNumber,
        collectorTitle = this.collectorTitle,
        collectorName = this.collectorName,
        collectionDate = this.collectionDate,
        collectionMethod = this.collectionMethod,
        specimenCondition = this.specimenCondition,
        createdAt = this.createdAt,
        completedAt = this.completedAt,
        submittedAt = this.submittedAt,
        notes = this.notes
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
        collectionMethod =this.collectionMethod,
        specimenCondition = this.specimenCondition,
        createdAt = this.createdAt,
        completedAt = this.completedAt,
        submittedAt = this.submittedAt,
        notes = this.notes
    )
}

fun Session.toDto(siteId: Int) : SessionDto {
    return SessionDto(
        localId = this.localId,
        siteId = siteId,
        remoteId =  this.remoteId,
        houseNumber = this.houseNumber,
        collectorTitle = this.collectorTitle,
        collectorName = this.collectorName,
        collectionDate = this.collectionDate,
        collectionMethod = this.collectionMethod,
        specimenCondition = this.specimenCondition,
        createdAt = this.createdAt,
        submittedAt = this.submittedAt,
        notes = this.notes
    )
}

fun SessionDto.toDomain() : Session {
    return Session(
        localId = this.localId,
        remoteId =  this.remoteId,
        houseNumber = this.houseNumber,
        collectorTitle = this.collectorTitle,
        collectorName = this.collectorName,
        collectionDate = this.collectionDate,
        collectionMethod = this.collectionMethod,
        specimenCondition = this.specimenCondition,
        createdAt = this.createdAt,
        completedAt = this.completedAt,
        submittedAt = this.submittedAt,
        notes = this.notes
    )
}