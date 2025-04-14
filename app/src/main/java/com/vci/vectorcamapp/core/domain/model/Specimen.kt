package com.vci.vectorcamapp.core.domain.model

data class Specimen(
    val id: String,
    val species: String,
    val sex: String,
    val abdomenStatus: String,
    val uri: String,
    val capturedAt: Long,
)
