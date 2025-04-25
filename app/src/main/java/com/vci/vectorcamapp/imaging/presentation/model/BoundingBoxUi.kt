package com.vci.vectorcamapp.imaging.presentation.model

data class BoundingBoxUi(
    val topLeftX: Float,
    val topLeftY: Float,
    val width: Float,
    val height: Float,
    val confidence: Float,
    val classId: Int,
)
