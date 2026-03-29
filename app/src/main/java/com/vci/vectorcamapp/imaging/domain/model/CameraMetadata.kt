package com.vci.vectorcamapp.imaging.domain.model

data class CameraMetadata(
    val focusDistance: Float? = null,
    val aperture: Float? = null,
    val exposureTimeNs: Long? = null,
    val iso: Int? = null,
    val colorCorrectionGains: ColorCorrectionGains? = null,
    val awbMode: Int? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val focalPointX: Float? = null,
    val focalPointY: Float? = null,
    val afRegions: List<AfRegion> = emptyList()
)

data class ColorCorrectionGains(
    val red: Float,
    val greenEven: Float,
    val greenOdd: Float,
    val blue: Float
)

data class AfRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val weight: Int
)
