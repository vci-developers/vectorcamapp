package com.vci.vectorcamapp.core.data.room.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity
import com.vci.vectorcamapp.core.data.room.entities.SpecimenEntity

data class SessionUnitWithSpecimensRelation(
    @Embedded val sessionUnitEntity: SessionUnitEntity,
    @Relation(
        parentColumn = "localId",
        entityColumn = "sessionUnitId"
    ) val specimenEntities: List<SpecimenEntity>,
)
