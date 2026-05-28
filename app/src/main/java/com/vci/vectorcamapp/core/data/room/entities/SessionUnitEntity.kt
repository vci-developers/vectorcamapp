package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "session_unit",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["localId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class SessionUnitEntity(
    @PrimaryKey val localId: UUID = UUID(0, 0),
    val sessionId: UUID = UUID(0, 0),
    val remoteId: Int? = null,
    val unitOrder: Int = 0,
    val createdAt: Long = 0L,
)
