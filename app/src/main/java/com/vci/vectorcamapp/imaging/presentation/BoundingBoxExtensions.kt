package com.vci.vectorcamapp.imaging.presentation

import android.content.res.Resources
import com.vci.vectorcamapp.imaging.domain.BoundingBox
import com.vci.vectorcamapp.imaging.domain.Detection

fun BoundingBox.toDetection(tensorWidth: Int, tensorHeight: Int, frameWidth: Int, frameHeight: Int): Detection {
    val screenHeight = Resources.getSystem().displayMetrics.heightPixels.toFloat()
    val screenWidth = Resources.getSystem().displayMetrics.widthPixels.toFloat()

    val previewHeight = screenHeight
    val previewWidth = (frameWidth.toFloat() / frameHeight.toFloat()) * previewHeight

    val scaleX = previewWidth / tensorWidth.toFloat()
    val scaleY = previewHeight / tensorHeight.toFloat()

    val xOffset = (previewWidth - screenWidth) / 2

    val scaledX = (this.topLeftX * tensorWidth * scaleX) - xOffset
    val scaledY = this.topLeftY * tensorHeight * scaleY
    val scaledWidth = this.width * tensorWidth * scaleX
    val scaledHeight = this.height * tensorHeight * scaleY

    return Detection(
        topLeftX = scaledX,
        topLeftY = scaledY,
        width = scaledWidth,
        height = scaledHeight,
        confidence = this.confidence,
        classId = this.classId,
    )
}
