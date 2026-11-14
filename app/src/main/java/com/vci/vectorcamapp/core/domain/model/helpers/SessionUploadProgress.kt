package com.vci.vectorcamapp.core.domain.model.helpers

import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus

data class SessionUploadProgress(
    val uploadedMetadataCount: Int,
    val uploadedImageCount: Int,
    val totalCount: Int,
    val failedImageCount: Int,
    val isUploading: Boolean,
    val metadataUploadStepStatuses: List<MetadataUploadStepStatus> = emptyList()
) {
    val hasFailedMetadataUploadStep: Boolean
        get() = metadataUploadStepStatuses.any {
            it.status == UploadStatus.FAILED || (it.status == UploadStatus.IN_PROGRESS && !isUploading)
        }

    val hasRecoveredMetadataUploadStep: Boolean
        get() = metadataUploadStepStatuses.any { it.status == UploadStatus.RECOVERED }
}
