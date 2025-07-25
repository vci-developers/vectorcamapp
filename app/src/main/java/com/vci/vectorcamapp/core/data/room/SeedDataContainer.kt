package com.vci.vectorcamapp.core.data.room

import com.google.gson.annotations.SerializedName
import com.vci.vectorcamapp.core.data.room.entities.ProgramEntity
import com.vci.vectorcamapp.core.data.room.entities.SiteEntity

data class SeedDataContainer(
    @SerializedName("programs") val programs: List<ProgramEntity>,
    @SerializedName("sites") val sites: List<SiteEntity>
)
