package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.dto.specimen_image.AfRegionDto
import com.vci.vectorcamapp.core.data.dto.specimen_image.ImageMetadataDto
import com.vci.vectorcamapp.core.data.room.entities.SpecimenImageEntity
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.AfRegion
import com.vci.vectorcamapp.core.domain.model.CameraMetadata
import com.vci.vectorcamapp.core.domain.model.ColorCorrectionGains
import java.util.UUID

fun SpecimenImageEntity.toDomain(): SpecimenImage {
    return SpecimenImage(
        localId = this.localId,
        remoteId = this.remoteId,
        species = this.species,
        sex = this.sex,
        abdomenStatus = this.abdomenStatus,
        imageUri = this.imageUri,
        metadataUploadStatus = this.metadataUploadStatus,
        imageUploadStatus = this.imageUploadStatus,
        capturedAt = this.capturedAt,
        submittedAt = this.submittedAt,
        imageMetadata = this.imageMetadata
    )
}

fun SpecimenImage.toEntity(specimenId: String, sessionId: UUID): SpecimenImageEntity {
    return SpecimenImageEntity(
        localId = this.localId,
        specimenId = specimenId,
        sessionId = sessionId,
        remoteId = this.remoteId,
        species = this.species,
        sex = this.sex,
        abdomenStatus = this.abdomenStatus,
        imageUri = this.imageUri,
        metadataUploadStatus = this.metadataUploadStatus,
        imageUploadStatus = this.imageUploadStatus,
        capturedAt = this.capturedAt,
        submittedAt = this.submittedAt,
        imageMetadata = this.imageMetadata
    )
}

fun CameraMetadata.toImageMetadataDto(): ImageMetadataDto = ImageMetadataDto(
    focusDistance = focusDistance,
    aperture = aperture,
    exposureTimeNs = exposureTimeNs,
    iso = iso,
    colorCorrectionGainsRed = colorCorrectionGains?.red,
    colorCorrectionGainsGreenEven = colorCorrectionGains?.greenEven,
    colorCorrectionGainsGreenOdd = colorCorrectionGains?.greenOdd,
    colorCorrectionGainsBlue = colorCorrectionGains?.blue,
    awbMode = awbMode,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    focalPointX = focalPointX,
    focalPointY = focalPointY,
    afRegions = afRegions.map { AfRegionDto(it.x, it.y, it.width, it.height, it.weight) }
)

internal fun ImageMetadataDto.toCameraMetadata(): CameraMetadata = CameraMetadata(
    focusDistance = focusDistance,
    aperture = aperture,
    exposureTimeNs = exposureTimeNs,
    iso = iso,
    colorCorrectionGains = if (colorCorrectionGainsRed != null) {
        ColorCorrectionGains(
            red = colorCorrectionGainsRed,
            greenEven = colorCorrectionGainsGreenEven ?: 0f,
            greenOdd = colorCorrectionGainsGreenOdd ?: 0f,
            blue = colorCorrectionGainsBlue ?: 0f
        )
    } else null,
    awbMode = awbMode,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    focalPointX = focalPointX,
    focalPointY = focalPointY,
    afRegions = afRegions.map { AfRegion(it.x, it.y, it.width, it.height, it.weight) }
)
