package com.vci.vectorcamapp.imaging.presentation

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults

data class ImagingState(
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val currentSpecimen: Specimen = Specimen(id = "", remoteId = null),
    val currentSpecimenImage: SpecimenImage = SpecimenImage(
        localId = "",
        remoteId = null,
        species = null,
        sex = null,
        abdomenStatus = null,
        imageUri = Uri.EMPTY,
        metadataUploadStatus = UploadStatus.NOT_STARTED,
        imageUploadStatus = UploadStatus.NOT_STARTED,
        capturedAt = 0L,
        submittedAt = null
    ),
    val currentInferenceResult: InferenceResult? = null,
    val currentImageBytes: ByteArray? = null,
    val previewInferenceResults: List<InferenceResult> = emptyList(),
    val specimensWithImagesAndInferenceResults: List<SpecimenWithSpecimenImagesAndInferenceResults> = emptyList(),
    val displayOrientation: Int = 0,
    val manualFocusPoint: Offset? = null,
    val isCameraReady: Boolean = false,
    val showExitDialog: Boolean = false,
    val pendingAction: ImagingAction? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImagingState

        if (isProcessing != other.isProcessing) return false
        if (displayOrientation != other.displayOrientation) return false
        if (isCameraReady != other.isCameraReady) return false
        if (currentSpecimen != other.currentSpecimen) return false
        if (currentSpecimenImage != other.currentSpecimenImage) return false
        if (currentInferenceResult != other.currentInferenceResult) return false
        if (currentImageBytes != null) {
            if (other.currentImageBytes == null) return false
            if (!currentImageBytes.contentEquals(other.currentImageBytes)) return false
        } else if (other.currentImageBytes != null) return false
        if (previewInferenceResults != other.previewInferenceResults) return false
        if (specimensWithImagesAndInferenceResults != other.specimensWithImagesAndInferenceResults) return false
        if (manualFocusPoint != other.manualFocusPoint) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isProcessing.hashCode()
        result = 31 * result + displayOrientation
        result = 31 * result + isCameraReady.hashCode()
        result = 31 * result + currentSpecimen.hashCode()
        result = 31 * result + currentSpecimenImage.hashCode()
        result = 31 * result + currentInferenceResult.hashCode()
        result = 31 * result + (currentImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + previewInferenceResults.hashCode()
        result = 31 * result + specimensWithImagesAndInferenceResults.hashCode()
        result = 31 * result + (manualFocusPoint?.hashCode() ?: 0)
        return result
    }
}
