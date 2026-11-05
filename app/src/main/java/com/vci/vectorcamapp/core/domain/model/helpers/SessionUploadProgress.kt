package com.vci.vectorcamapp.core.domain.model.helpers

import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus

data class SessionUploadProgress(
    val uploadedMetadataCount: Int,
    val uploadedImageCount: Int,
    val totalCount: Int,
    val failedImageCount: Int,
    val isUploading: Boolean,
    val metadataUploadStepStatuses: List<MetadataUploadStepStatus> = emptyList()
)
