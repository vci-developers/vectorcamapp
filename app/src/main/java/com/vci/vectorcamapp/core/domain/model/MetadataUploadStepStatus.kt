package com.vci.vectorcamapp.core.domain.model

import com.vci.vectorcamapp.core.domain.model.enums.MetadataUploadStep
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus

data class MetadataUploadStepStatus(
    val step: MetadataUploadStep,
    val status: UploadStatus,
    val updatedAt: Long
)
