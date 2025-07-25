package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "program", indices = [Index("country")])
@Serializable
data class ProgramEntity(
    @PrimaryKey val id: Int = -1,
    val name: String = "",
    val country: String = "",
)
