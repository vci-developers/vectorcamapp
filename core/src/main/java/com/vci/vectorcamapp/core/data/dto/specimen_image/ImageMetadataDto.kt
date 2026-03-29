package com.vci.vectorcamapp.core.data.dto.specimen_image

import kotlinx.serialization.Serializable

@Serializable
data class ImageMetadataDto(
    val focusDistance: Float? = null,
    val aperture: Float? = null,
    val exposureTimeNs: Long? = null,
    val iso: Int? = null,
    val colorCorrectionGainsRed: Float? = null,
    val colorCorrectionGainsGreenEven: Float? = null,
    val colorCorrectionGainsGreenOdd: Float? = null,
    val colorCorrectionGainsBlue: Float? = null,
    val awbMode: Int? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val focalPointX: Float? = null,
    val focalPointY: Float? = null,
    val afRegions: List<AfRegionDto> = emptyList()
)

@Serializable
data class AfRegionDto(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val weight: Int
)
