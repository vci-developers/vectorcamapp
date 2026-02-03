package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "site", foreignKeys = [ForeignKey(
        entity = ProgramEntity::class,
        parentColumns = ["id"],
        childColumns = ["programId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )],
    indices = [
        Index("programId"),
        Index("locationTypeId"),
        Index("parentId")
    ]
)
data class SiteEntity(
    @PrimaryKey val id: Int = -1,
    val programId: Int = -1,
    val district: String? = null,
    val subCounty: String? = null,
    val parish: String? = null,
    val villageName: String? = null,
    val houseNumber: String? = null,
    val healthCenter: String? = null,
    val isActive: Boolean = true,
    val locationTypeId: Int? = null,
    val parentId: Int? = null,
    val name: String? = null,
    val locationHierarchy: Map<String, String>? = null
)
