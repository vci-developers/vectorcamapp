package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bounding_box", foreignKeys = [
        ForeignKey(
            entity = SpecimenEntity::class,
            parentColumns = ["id"],
            childColumns = ["specimenId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class BoundingBoxEntity(
    @PrimaryKey val specimenId: String = "",
    val topLeftX: Float = 0F,
    val topLeftY: Float = 0F,
    val width: Float = 0F,
    val height: Float = 0F,
    val confidence: Float = 0F,
    val classId: Int = 0,
)
