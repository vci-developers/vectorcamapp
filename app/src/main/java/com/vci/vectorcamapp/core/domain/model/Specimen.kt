package com.vci.vectorcamapp.core.domain.model

import java.util.UUID

data class Specimen(
    val id: String,
    val remoteId: Int?,
    val shouldProcessFurther: Boolean,
    val sessionUnitId: UUID? = null,
)
