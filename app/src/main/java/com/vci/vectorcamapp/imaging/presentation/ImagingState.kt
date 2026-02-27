package com.vci.vectorcamapp.imaging.presentation

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.imaging.domain.util.ImagingError

data class ImagingState(
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val currentSpecimen: Specimen = Specimen(id = "", remoteId = null, shouldProcessFurther = false, expectedImages = 0),
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
    val focusPoint: Offset? = null,
    val allowModelInferenceToggle: Boolean = false,
    val shouldRunInference: Boolean = true,
    val isManualFocusing: Boolean = false,
    val isCameraReady: Boolean = false,
    val showExitDialog: Boolean = false,
    val showProcessFurtherDialog: Boolean = false,
    val hasConfirmedPackaging: Boolean = false,
    val pendingAction: ImagingAction? = null,
    val specimenIdError: ImagingError? = null
)
