package com.vci.vectorcamapp.imaging.domain.util

import androidx.annotation.StringRes
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class ImagingError(@StringRes override val messageResId: Int) : Error {
    CAPTURE_ERROR(R.string.imaging_error_capture_error),
    SAVE_ERROR(R.string.imaging_error_save_error),
    PROCESSING_ERROR(R.string.imaging_error_processing_error),
    NO_SPECIMEN_FOUND(R.string.imaging_error_no_specimen_found),
    MULTIPLE_SPECIMENS_FOUND(R.string.imaging_error_multiple_specimens_found),
    NO_ACTIVE_SESSION(R.string.imaging_error_no_active_session),
    MODEL_INITIALIZATION_FAILED(R.string.imaging_error_model_initialization_failed),
    GPU_DELEGATE_INITIALIZATION_FAILED(R.string.imaging_error_gpu_delegate_initialization_failed),
    UNKNOWN_INITIALIZATION_ERROR(R.string.imaging_error_unknown_initialization_error),
    INVALID_INPUT_SHAPE(R.string.imaging_error_invalid_input_shape),
    SPECIMEN_ID_RECOGNITION_FAILED(R.string.imaging_error_specimen_id_recognition_failed),
    SPECIMEN_DETECTION_FAILED(R.string.imaging_error_specimen_detection_failed),
    SPECIMEN_CLASSIFICATION_FAILED(R.string.imaging_error_specimen_classification_failed),
    UNKNOWN_INFERENCE_ERROR(R.string.imaging_error_unknown_inference_error),
    UNKNOWN_ERROR(R.string.imaging_error_unknown_error);
}
