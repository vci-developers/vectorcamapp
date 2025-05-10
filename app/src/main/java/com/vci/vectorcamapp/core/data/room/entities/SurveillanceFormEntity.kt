package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "surveillance_form", foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )]
)
data class SurveillanceFormEntity(
    @PrimaryKey val sessionId: UUID = UUID(0, 0),
    val country: String = "",
    val district: String = "",
    val healthCenter: String = "",
    val sentinelSite: String = "",
    val householdNumber: String = "",
    val latitude: Float = 0F,
    val longitude: Float = 0F,
    val collectionDate: Long = 0L,
    val collectionMethod: String = "",
    val collectorName: String = "",
    val collectorTitle: String = "",
    val numPeopleSleptInHouse: Int = 0,
    val wasIrsConducted: Boolean = false,
    val monthsSinceIrs: Int? = null,
    val numLlinsAvailable: Int = 0,
    val llinType: String? = null,
    val llinBrand: String? = null,
    val numPeopleSleptUnderLlin: Int? = null,
    val notes: String = ""
)
