package com.vci.vectorcamapp.imaging.domain.model

import androidx.compose.ui.geometry.Offset
import com.vci.vectorcamapp.core.domain.model.InferenceResult

data class LiveFrameProcessingResult(
    val specimenId: String = "",
    val previewInferenceResults: List<InferenceResult> = emptyList(),
    val autofocusPoint: Offset? = null
)
