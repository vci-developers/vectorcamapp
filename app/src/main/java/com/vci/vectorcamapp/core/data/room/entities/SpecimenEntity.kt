package com.vci.vectorcamapp.core.data.room.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "specimen", foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["localId"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )], indices = [Index("sessionId")]
)
data class SpecimenEntity(
    @PrimaryKey val id: String = "",
    val sessionId: UUID = UUID(0, 0),
    val species: String? = null,
    val sex: String? = null,
    val abdomenStatus: String? = null,
    val imageUri: Uri = Uri.EMPTY,
    val capturedAt: Long = 0L,
)
