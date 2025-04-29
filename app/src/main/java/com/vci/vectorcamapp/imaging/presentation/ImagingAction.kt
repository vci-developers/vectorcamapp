package com.vci.vectorcamapp.imaging.presentation

import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController

sealed interface ImagingAction {
    data class CorrectSpecimenId(val specimenId: String) : ImagingAction
    data class ProcessFrame(val frame: ImageProxy) : ImagingAction
    data object SaveSessionProgress : ImagingAction
    data object SubmitSession : ImagingAction
    data class CaptureImage(val controller: LifecycleCameraController) : ImagingAction
    data object SaveImageToSession : ImagingAction
    data object RetakeImage : ImagingAction
}
