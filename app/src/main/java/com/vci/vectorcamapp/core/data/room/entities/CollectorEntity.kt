package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "collector")
data class CollectorEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val title: String = "",
    val lastTrainedOn: Long = 0L,
)
