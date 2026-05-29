package com.vci.vectorcamapp.core.data.room.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.vci.vectorcamapp.core.data.room.entities.SessionEntity
import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity

data class SessionWithSessionUnitsRelation(
    @Embedded val sessionEntity: SessionEntity,
    @Relation(
        parentColumn = "localId",
        entityColumn = "sessionId"
    )
    val sessionUnitEntities: List<SessionUnitEntity>
)
