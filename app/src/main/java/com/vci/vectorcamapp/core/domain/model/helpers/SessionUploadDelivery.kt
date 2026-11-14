package com.vci.vectorcamapp.core.domain.model.helpers

import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus
import com.vci.vectorcamapp.core.domain.model.SpecimenImage

object SessionUploadDelivery {
    fun isFullyDelivered(
        submittedAt: Long?,
        metadataSteps: List<MetadataUploadStepStatus>,
        images: List<SpecimenImage>,
    ): Boolean {
        if (images.any { !it.metadataUploadStatus.isUploaded() || !it.imageUploadStatus.isUploaded() }) {
            return false
        }
        return areMetadataStepsDelivered(submittedAt, metadataSteps)
    }

    fun isFullyDelivered(
        submittedAt: Long?,
        progress: SessionUploadProgress,
    ): Boolean {
        if (progress.failedImageCount > 0) return false
        if (!progress.areImageFilesDelivered || !progress.areImageMetadataDelivered) return false
        return areMetadataStepsDelivered(submittedAt, progress.metadataUploadStepStatuses)
    }

    fun hasStarted(submittedAt: Long?, progress: SessionUploadProgress): Boolean {
        return submittedAt != null ||
            progress.metadataUploadStepStatuses.isNotEmpty() ||
            progress.uploadedMetadataCount > 0 ||
            progress.uploadedImageCount > 0 ||
            progress.isUploading
    }

    private fun areMetadataStepsDelivered(
        submittedAt: Long?,
        metadataSteps: List<MetadataUploadStepStatus>,
    ): Boolean {
        if (metadataSteps.isEmpty()) return submittedAt != null
        return metadataSteps.all { it.status.isUploaded() }
    }
}
