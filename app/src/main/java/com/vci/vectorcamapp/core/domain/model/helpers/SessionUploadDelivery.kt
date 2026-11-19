package com.vci.vectorcamapp.core.domain.model.helpers

import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus
import com.vci.vectorcamapp.core.domain.model.SpecimenImage

object SessionUploadDelivery {
    fun isFullyDelivered(
        metadataSteps: List<MetadataUploadStepStatus>,
        images: List<SpecimenImage>,
    ): Boolean {
        if (images.any { !it.metadataUploadStatus.isUploaded() || !it.imageUploadStatus.isUploaded() }) {
            return false
        }
        return areMetadataStepsDelivered(metadataSteps)
    }

    fun isFullyDelivered(progress: SessionUploadProgress): Boolean {
        if (progress.failedImageCount > 0) return false
        if (!progress.areImageFilesDelivered || !progress.areImageMetadataDelivered) return false
        return areMetadataStepsDelivered(progress.metadataUploadStepStatuses)
    }

    fun hasStarted(progress: SessionUploadProgress): Boolean {
        return progress.hasUploadHistory ||
            progress.uploadedMetadataCount > 0 ||
            progress.uploadedImageCount > 0 ||
            progress.isUploading
    }

    private fun areMetadataStepsDelivered(
        metadataSteps: List<MetadataUploadStepStatus>,
    ): Boolean {
        // Empty history is not complete: older builds set submittedAt without step records.
        if (metadataSteps.isEmpty()) return false
        return metadataSteps.all { it.status.isUploaded() }
    }
}
