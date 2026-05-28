package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity
import com.vci.vectorcamapp.core.domain.model.SessionUnit

fun SessionUnitEntity.toDomain() = SessionUnit(
    localId = localId,
    sessionId = sessionId,
    remoteId = remoteId,
    unitOrder = unitOrder,
    createdAt = createdAt,
)

fun SessionUnit.toEntity() = SessionUnitEntity(
    localId = localId,
    sessionId = sessionId,
    remoteId = remoteId,
    unitOrder = unitOrder,
    createdAt = createdAt,
)
