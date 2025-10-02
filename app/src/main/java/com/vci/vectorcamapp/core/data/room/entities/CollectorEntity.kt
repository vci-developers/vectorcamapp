package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collector")
data class CollectorEntity(
    @PrimaryKey val id: Int = -1,
    val name: String = "",
    val title: String = ""
)
