package com.vci.vectorcamapp.core.domain.model

data class BoundingBox(
    val topLeftX: Float,
    val topLeftY: Float,
    val width: Float,
    val height: Float,
    val confidence: Float,
    val classId: Int,
)
