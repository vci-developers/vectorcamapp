package com.vci.vectorcamapp.core.data.room.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.vci.vectorcamapp.core.data.room.entities.SpecimenEntity
import com.vci.vectorcamapp.core.data.room.entities.SpecimenImageEntity

data class SpecimenWithSpecimenImagesAndInferenceResultsRelation(
    @Embedded val specimenEntity: SpecimenEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "specimenId",
        entity = SpecimenImageEntity::class
    )
    val specimenImageAndInferenceResultRelations: List<SpecimenImageAndInferenceResultRelation>
)
