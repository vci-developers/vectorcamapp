package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "form",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("programId")
    ]
)
data class FormEntity(
    @PrimaryKey val id: Int = -1,
    val programId: Int = -1,
    val name: String,
    val version: String
)
