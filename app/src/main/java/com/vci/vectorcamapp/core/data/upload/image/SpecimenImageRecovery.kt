package com.vci.vectorcamapp.core.data.upload.image

import com.vci.vectorcamapp.core.data.dto.data_recovery.PostStaleSpecimenImageRecoveryRequestDto
import com.vci.vectorcamapp.core.data.dto.data_recovery.StaleSpecimenImagePayloadDto
import com.vci.vectorcamapp.core.data.network.api.UploadRoutes
import java.util.UUID

object SpecimenImageRecovery {
    fun shouldUseRecoveryTus(imageRemoteId: Int?): Boolean = imageRemoteId == null

    fun idempotencyKey(deviceId: Int, imageLocalId: String): String =
        "recovery-image-$deviceId-$imageLocalId"

    fun tusPath(recoveryId: Int): String = UploadRoutes.dataRecoveryTus(recoveryId)

    fun fingerprint(recoveryId: Int, filemd5: String): String =
        "data-recovery-$recoveryId-$filemd5"

    fun request(
        deviceId: Int,
        imageLocalId: String,
        sessionFrontendId: UUID,
        programId: Int?,
        siteId: Int?,
        filemd5: String,
        contentType: String,
        clientSpecimenId: String,
        serverSpecimenId: Int?,
        imageId: Int?,
    ): PostStaleSpecimenImageRecoveryRequestDto {
        return PostStaleSpecimenImageRecoveryRequestDto(
            idempotencyKey = idempotencyKey(deviceId, imageLocalId),
            status = PostStaleSpecimenImageRecoveryRequestDto.STATUS_PENDING_UPLOAD,
            sessionFrontendId = sessionFrontendId,
            programId = programId,
            siteId = siteId,
            payload = StaleSpecimenImagePayloadDto(
                type = StaleSpecimenImagePayloadDto.TYPE_STALE_SPECIMEN_IMAGE,
                filemd5 = filemd5,
                contentType = contentType,
                clientSpecimenId = clientSpecimenId,
                serverSpecimenId = serverSpecimenId,
                imageId = imageId,
            ),
        )
    }
}
