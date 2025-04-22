package com.vci.vectorcamapp.core.data.room.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.vci.vectorcamapp.core.data.room.entities.BoundingBoxEntity
import com.vci.vectorcamapp.core.data.room.entities.SpecimenEntity

data class SpecimenAndBoundingBoxRelation(
    @Embedded val specimen: SpecimenEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "specimenId"
    )
    val boundingBox: BoundingBoxEntity
)
