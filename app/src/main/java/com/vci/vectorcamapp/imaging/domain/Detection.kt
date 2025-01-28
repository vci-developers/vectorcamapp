package com.vci.vectorcamapp.imaging.domain

data class Detection(
    val topLeftX: Float = 0f,
    val topLeftY: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val confidence: Float = 0f,
    val classId: Int = 0,
)
