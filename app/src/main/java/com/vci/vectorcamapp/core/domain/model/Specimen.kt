package com.vci.vectorcamapp.core.domain.model

data class Specimen(
    val id: String,
    val remoteId: Int?,
    val shouldProcessFurther: Boolean,
    val expectedImages: Int
)
