package com.vci.vectorcamapp.core.domain.model.helpers

import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus

data class SessionUploadProgress(
    val uploadedMetadataCount: Int,
    val uploadedImageCount: Int,
    val totalCount: Int,
    val failedImageCount: Int,
    val isUploading: Boolean,
    val metadataUploadStepStatuses: List<MetadataUploadStepStatus> = emptyList(),
    val recoveredImageCount: Int = 0,
) {
    val areImageFilesDelivered: Boolean
        get() = totalCount == 0 || uploadedImageCount == totalCount

    val areImageMetadataDelivered: Boolean
        get() = totalCount == 0 || uploadedMetadataCount == totalCount

    val hasFailedMetadataUploadStep: Boolean
        get() = metadataUploadStepStatuses.any {
            it.status == UploadStatus.FAILED || (it.status == UploadStatus.IN_PROGRESS && !isUploading)
        }

    val hasRecoveredMetadataUploadStep: Boolean
        get() = metadataUploadStepStatuses.any { it.status == UploadStatus.RECOVERED }

    val hasRecoveredDelivery: Boolean
        get() = hasRecoveredMetadataUploadStep || recoveredImageCount > 0

    val hasUploadHistory: Boolean
        get() = metadataUploadStepStatuses.isNotEmpty()
}
