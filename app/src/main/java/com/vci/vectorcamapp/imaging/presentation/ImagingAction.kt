package com.vci.vectorcamapp.imaging.presentation

import android.view.Surface
import androidx.compose.ui.geometry.Offset
import com.vci.vectorcamapp.imaging.domain.camera.DisplayRotation

sealed interface ImagingAction {
    data class CorrectSpecimenId(val specimenId: String) : ImagingAction
    data object SaveSessionProgress : ImagingAction
    data object SubmitSession : ImagingAction
    data class ToggleModelInference(val isChecked: Boolean) : ImagingAction
    data object CaptureImage : ImagingAction
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
    data class AttachSurface(val surface: Surface, val displayRotation: DisplayRotation) : ImagingAction
    data object DetachSurface : ImagingAction
}
