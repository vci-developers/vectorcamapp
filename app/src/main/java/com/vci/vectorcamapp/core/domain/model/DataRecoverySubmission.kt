package com.vci.vectorcamapp.core.domain.model

import com.vci.vectorcamapp.core.domain.model.enums.MetadataUploadStep

data class DataRecoverySubmission(
    val idempotencyKey: String,
    val step: MetadataUploadStep,
    val dataRecoveryId: Int?,
    val submittedAt: Long,
)
