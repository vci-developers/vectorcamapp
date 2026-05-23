package com.vci.vectorcamapp.imaging.domain.model

data class CapturedImage(
    val jpegBytes: ByteArray,
    val metadata: CameraMetadata
)
