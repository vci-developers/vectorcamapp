package com.vci.vectorcamapp.core.data.dto.inference_result

import kotlinx.serialization.Serializable

@Serializable
data class InferenceResultDto(
    val bboxTopLeftX: Float = 0f,
    val bboxTopLeftY: Float = 0f,
    val bboxWidth: Float = 0f,
    val bboxHeight: Float = 0f,
    val bboxConfidence: Float = 0f,
    val bboxClassId: Int = 0,
    val speciesLogits: List<Float>? = null,
    val sexLogits: List<Float>? = null,
    val abdomenStatusLogits: List<Float>? = null,
    val bboxDetectionDuration: Long = 0L,
    val speciesInferenceDuration: Long? = null,
    val sexInferenceDuration: Long? = null,
    val abdomenStatusInferenceDuration: Long? = null,
)
