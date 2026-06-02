package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import java.util.UUID

fun SessionUnitEntity.toDomain(): SessionUnit {
    return SessionUnit(
        localId = this.localId,
        remoteId = this.remoteId,
        unitOrder = this.unitOrder,
        createdAt = this.createdAt,
    )
}

fun SessionUnit.toEntity(sessionId: UUID): SessionUnitEntity {
    return SessionUnitEntity(
        localId = this.localId,
        sessionId = sessionId,
        remoteId = this.remoteId,
        unitOrder = this.unitOrder,
        createdAt = this.createdAt,
    )
}
