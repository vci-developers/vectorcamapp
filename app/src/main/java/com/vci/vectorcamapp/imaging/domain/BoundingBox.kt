package com.vci.vectorcamapp.imaging.domain

data class BoundingBox(
    val topLeftX: Float = 0f,
    val topLeftY: Float = 0f,
    val bottomRightX: Float = 0f,
    val bottomRightY: Float = 0f,
    val centerX: Float = 0f,
    val centerY: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val confidence: Float = 0f,
    val classId: Int = 0,
)

