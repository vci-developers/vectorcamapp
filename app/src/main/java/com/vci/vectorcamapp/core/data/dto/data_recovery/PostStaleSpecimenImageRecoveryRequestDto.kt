package com.vci.vectorcamapp.core.data.dto.data_recovery

import com.vci.vectorcamapp.core.data.dto.serializers.UuidSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PostStaleSpecimenImageRecoveryRequestDto(
    val idempotencyKey: String = "",
    val status: String = STATUS_PENDING_UPLOAD,
    @Serializable(with = UuidSerializer::class)
    val sessionFrontendId: UUID = UUID(0, 0),
    val programId: Int? = null,
    val siteId: Int? = null,
    val payload: StaleSpecimenImagePayloadDto = StaleSpecimenImagePayloadDto(),
) {
    companion object {
        const val STATUS_PENDING_UPLOAD = "pending_upload"
    }
}

@Serializable
data class StaleSpecimenImagePayloadDto(
    val type: String = TYPE_STALE_SPECIMEN_IMAGE,
    val filemd5: String = "",
    val contentType: String = "",
    val clientSpecimenId: String = "",
    val serverSpecimenId: Int? = null,
    val imageId: Int? = null,
) {
    companion object {
        const val TYPE_STALE_SPECIMEN_IMAGE = "stale_specimen_image"
    }
}
