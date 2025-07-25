package com.vci.vectorcamapp.core.data.room

import com.vci.vectorcamapp.core.data.room.entities.ProgramEntity
import com.vci.vectorcamapp.core.data.room.entities.SiteEntity
import kotlinx.serialization.Serializable

@Serializable
data class SeedDataContainer(
    val programs: List<ProgramEntity> = emptyList(),
    val sites: List<SiteEntity> = emptyList()
)
