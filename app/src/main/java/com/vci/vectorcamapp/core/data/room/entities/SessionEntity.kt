package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "session", indices = [Index("submittedAt")]
)
data class SessionEntity(
    @PrimaryKey val id: UUID = UUID(0, 0),
    val createdAt: Long = 0L,
    val submittedAt: Long? = null,
)
