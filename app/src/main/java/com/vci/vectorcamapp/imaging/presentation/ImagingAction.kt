package com.vci.vectorcamapp.imaging.presentation

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset

sealed interface ImagingAction {
    data class CorrectSpecimenId(val specimenId: String) : ImagingAction
    data class ProcessFrame(val frame: ImageProxy) : ImagingAction
    data object SaveSessionProgress : ImagingAction
    data object SubmitSession : ImagingAction
    data class ToggleModelInference(val isChecked: Boolean) : ImagingAction
    data class CaptureImage(val imageCapture: ImageCapture) : ImagingAction
    data object SaveImageToSession : ImagingAction
    data object RetakeImage : ImagingAction
    data class FocusAt(val offset: Offset) : ImagingAction
    data object CancelFocus : ImagingAction
    data object ShowExitDialog : ImagingAction
    data object DismissExitDialog : ImagingAction
    data class TogglePackagingConfirmation(val isChecked: Boolean) : ImagingAction
    data class SelectPendingAction(val pendingAction: ImagingAction) : ImagingAction
    data object ClearPendingAction : ImagingAction
    data object ConfirmPendingAction : ImagingAction
}
