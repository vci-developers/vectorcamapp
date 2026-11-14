package com.vci.vectorcamapp.core.domain.model.helpers

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.enums.MetadataUploadStep
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import io.mockk.mockk
import org.junit.Test

class SessionUploadDeliveryTest {

    private val recoveredSessionStep = MetadataUploadStepStatus(
        MetadataUploadStep.SESSION,
        UploadStatus.RECOVERED,
        1L,
    )
    private val completedSessionStep = MetadataUploadStepStatus(
        MetadataUploadStep.SESSION,
        UploadStatus.COMPLETED,
        1L,
    )
    private val failedFormStep = MetadataUploadStepStatus(
        MetadataUploadStep.FORM_ANSWERS,
        UploadStatus.FAILED,
        1L,
    )

    @Test
    fun emptyStepsWithoutSubmittedAt_isNotDelivered() {
        assertThat(SessionUploadDelivery.isFullyDelivered(null, emptyList(), emptyList())).isFalse()
    }

    @Test
    fun emptyStepsWithSubmittedAtAndNoImages_isDelivered() {
        assertThat(SessionUploadDelivery.isFullyDelivered(9L, emptyList(), emptyList())).isTrue()
    }

    @Test
    fun recoveredStepsWithoutSubmittedAt_isDelivered() {
        assertThat(
            SessionUploadDelivery.isFullyDelivered(null, listOf(recoveredSessionStep), emptyList())
        ).isTrue()
    }

    @Test
    fun recoveredMetadataWithoutImageFile_isNotDelivered() {
        val image = specimenImage(
            metadataUploadStatus = UploadStatus.RECOVERED,
            imageUploadStatus = UploadStatus.NOT_STARTED,
        )
        assertThat(
            SessionUploadDelivery.isFullyDelivered(null, listOf(recoveredSessionStep), listOf(image))
        ).isFalse()
    }

    @Test
    fun recoveredMetadataAndRecoveredFile_isDelivered() {
        val image = specimenImage(
            metadataUploadStatus = UploadStatus.RECOVERED,
            imageUploadStatus = UploadStatus.RECOVERED,
        )
        assertThat(
            SessionUploadDelivery.isFullyDelivered(null, listOf(recoveredSessionStep), listOf(image))
        ).isTrue()
    }

    @Test
    fun mixedNormalAndRecovery_isDelivered() {
        val image = specimenImage(
            metadataUploadStatus = UploadStatus.COMPLETED,
            imageUploadStatus = UploadStatus.RECOVERED,
        )
        assertThat(
            SessionUploadDelivery.isFullyDelivered(
                9L,
                listOf(completedSessionStep, recoveredSessionStep.copy(step = MetadataUploadStep.FORM_ANSWERS)),
                listOf(image),
            )
        ).isTrue()
    }

    @Test
    fun failedMetadataStep_isNotDelivered() {
        assertThat(
            SessionUploadDelivery.isFullyDelivered(
                9L,
                listOf(completedSessionStep, failedFormStep),
                emptyList(),
            )
        ).isFalse()
    }

    @Test
    fun progressCountsTreatRecoveredImagesAsDelivered() {
        val progress = SessionUploadProgress(
            uploadedMetadataCount = 1,
            uploadedImageCount = 1,
            totalCount = 1,
            failedImageCount = 0,
            isUploading = false,
            metadataUploadStepStatuses = listOf(recoveredSessionStep),
        )
        assertThat(SessionUploadDelivery.isFullyDelivered(null, progress)).isTrue()
        assertThat(progress.areImageFilesDelivered).isTrue()
    }

    @Test
    fun recoveredMetadataProgressWithoutImageCount_isNotDelivered() {
        val progress = SessionUploadProgress(
            uploadedMetadataCount = 1,
            uploadedImageCount = 0,
            totalCount = 1,
            failedImageCount = 0,
            isUploading = false,
            metadataUploadStepStatuses = listOf(recoveredSessionStep),
        )
        assertThat(SessionUploadDelivery.isFullyDelivered(null, progress)).isFalse()
        assertThat(SessionUploadDelivery.hasStarted(null, progress)).isTrue()
    }

    private fun specimenImage(
        metadataUploadStatus: UploadStatus,
        imageUploadStatus: UploadStatus,
    ) = SpecimenImage(
        localId = "img",
        remoteId = null,
        species = null,
        sex = null,
        abdomenStatus = null,
        imageUri = mockk<Uri>(),
        metadataUploadStatus = metadataUploadStatus,
        imageUploadStatus = imageUploadStatus,
        capturedAt = 0L,
        submittedAt = null,
        imageMetadata = null,
    )
}
