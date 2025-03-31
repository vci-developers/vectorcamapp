package com.vci.vectorcamapp.core.domain.model

import java.util.Date

data class Specimen(
    val id: String,
    val species: String,
    val sex: String,
    val abdomenStatus: String,
    val uri: String,
    val capturedAt: Date,
)
