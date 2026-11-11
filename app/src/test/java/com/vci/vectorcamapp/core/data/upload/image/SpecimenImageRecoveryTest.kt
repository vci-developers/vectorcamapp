package com.vci.vectorcamapp.core.data.upload.image

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.dto.data_recovery.PostStaleSpecimenImageRecoveryRequestDto
import com.vci.vectorcamapp.core.data.dto.data_recovery.StaleSpecimenImagePayloadDto
import org.junit.Test
import java.util.UUID

class SpecimenImageRecoveryTest {

    @Test
    fun shouldUseRecoveryTus_whenImageHasNoRemoteId() {
        assertThat(SpecimenImageRecovery.shouldUseRecoveryTus(null)).isTrue()
        assertThat(SpecimenImageRecovery.shouldUseRecoveryTus(42)).isFalse()
    }

    @Test
    fun request_usesPendingUploadPayloadAndLocalImageIdempotencyKey() {
        val sessionId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val request = SpecimenImageRecovery.request(
            deviceId = 9,
            imageLocalId = "md5-local",
            sessionFrontendId = sessionId,
            programId = 4,
            siteId = 7,
            filemd5 = "d41d8cd98f00b204e9800998ecf8427e",
            contentType = "image/jpeg",
            clientSpecimenId = "specimen-1",
            serverSpecimenId = null,
            imageId = null,
        )

        assertThat(request.idempotencyKey).isEqualTo("recovery-image-9-md5-local")
        assertThat(request.status)
            .isEqualTo(PostStaleSpecimenImageRecoveryRequestDto.STATUS_PENDING_UPLOAD)
        assertThat(request.sessionFrontendId).isEqualTo(sessionId)
        assertThat(request.programId).isEqualTo(4)
        assertThat(request.siteId).isEqualTo(7)
        assertThat(request.payload.type)
            .isEqualTo(StaleSpecimenImagePayloadDto.TYPE_STALE_SPECIMEN_IMAGE)
        assertThat(request.payload.filemd5).isEqualTo("d41d8cd98f00b204e9800998ecf8427e")
        assertThat(request.payload.contentType).isEqualTo("image/jpeg")
        assertThat(request.payload.clientSpecimenId).isEqualTo("specimen-1")
        assertThat(request.payload.serverSpecimenId).isNull()
        assertThat(request.payload.imageId).isNull()
        assertThat(SpecimenImageRecovery.tusPath(15)).isEqualTo("data-recoveries/15/tus")
        assertThat(SpecimenImageRecovery.fingerprint(15, request.payload.filemd5))
            .isEqualTo("data-recovery-15-d41d8cd98f00b204e9800998ecf8427e")
    }
}
