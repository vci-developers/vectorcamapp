package com.vci.vectorcamapp.core.domain.model.results

data class DetectorResult(
    val bboxTopLeftX: Float,
    val bboxTopLeftY: Float,
    val bboxWidth: Float,
    val bboxHeight: Float,
    val bboxConfidence: Float,
    val bboxClassId: Int,
    val bboxDetectionDuration: Long,
)
