package com.vci.vectorcamapp.imaging.domain.model

data class FocusStackResult(
    val compositeJpeg: ByteArray,
    val frameJpegs: List<ByteArray>,
    val width: Int,
    val height: Int
)
