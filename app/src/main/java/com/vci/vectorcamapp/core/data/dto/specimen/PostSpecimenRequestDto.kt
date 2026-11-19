package com.vci.vectorcamapp.core.data.dto.specimen

import com.vci.vectorcamapp.core.data.dto.serializers.UuidSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PostSpecimenRequestDto(
    val specimenId: String = "",
    val sessionUnitId: Int? = null,
    @Serializable(with = UuidSerializer::class)
    val sessionUnitFrontendId: UUID? = null,
    val shouldProcessFurther: Boolean = false,
    val expectedImages: Int = 0,
)
