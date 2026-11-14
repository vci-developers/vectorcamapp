package com.vci.vectorcamapp.core.data.upload

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenImageAndInferenceResult
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.model.enums.MetadataUploadStep
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.repository.MetadataUploadStatusRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class SessionUploadSchedulerTest {

    private val sessionId = UUID.randomUUID()
    private val session = Session(
        localId = sessionId,
        remoteId = null,
        hardwareId = "HW",
        collectorTitle = "Dr.",
        collectorName = "Ada",
        collectorLastTrainedOn = 0L,
        collectionDate = 1L,
        collectionMethod = "Net",
        specimenCondition = "Good",
        createdAt = 1L,
        completedAt = 2L,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        gpsAccuracy = null,
        type = SessionType.SURVEILLANCE,
    )
    private val site = Site(
        id = 4,
        district = "D",
        subCounty = "S",
        parish = "P",
        villageName = "V",
        houseNumber = "1",
        healthCenter = "H",
        isActive = true,
        name = null,
        locationHierarchy = null,
        latitude = null,
        longitude = null,
    )

    @Test
    fun enqueuePendingCompleteSessions_skipsFullyRecoveredSession() = runTest {
        val scheduler = scheduler(
            images = listOf(specimenImage(UploadStatus.RECOVERED, UploadStatus.RECOVERED)),
            steps = listOf(
                MetadataUploadStepStatus(MetadataUploadStep.SESSION, UploadStatus.RECOVERED, 1L),
                MetadataUploadStepStatus(MetadataUploadStep.SPECIMEN_IMAGES, UploadStatus.RECOVERED, 1L),
            ),
        )

        val enqueued = scheduler.enqueuePendingCompleteSessions()

        assertThat(enqueued).isEqualTo(0)
        coVerify(exactly = 0) { schedulerWorkManager.enqueueSessionUpload(any(), any()) }
    }

    @Test
    fun enqueuePendingCompleteSessions_enqueuesWhenImageFileStillPending() = runTest {
        val scheduler = scheduler(
            images = listOf(specimenImage(UploadStatus.RECOVERED, UploadStatus.NOT_STARTED)),
            steps = listOf(
                MetadataUploadStepStatus(MetadataUploadStep.SESSION, UploadStatus.RECOVERED, 1L),
                MetadataUploadStepStatus(MetadataUploadStep.SPECIMEN_IMAGES, UploadStatus.RECOVERED, 1L),
            ),
        )

        val enqueued = scheduler.enqueuePendingCompleteSessions()

        assertThat(enqueued).isEqualTo(1)
        coVerify(exactly = 1) { schedulerWorkManager.enqueueSessionUpload(sessionId, site.id) }
    }

    private lateinit var schedulerWorkManager: WorkManagerRepository

    private fun scheduler(
        images: List<SpecimenImage>,
        steps: List<MetadataUploadStepStatus>,
    ): SessionUploadScheduler {
        val sessionRepository = mockk<SessionRepository>()
        every { sessionRepository.observeCompleteSessionsAndSites() } returns flowOf(
            listOf(SessionAndSite(session, site))
        )
        val specimenRepository = mockk<SpecimenRepository>()
        coEvery {
            specimenRepository.getSpecimenImagesAndInferenceResultsBySessionScope(sessionId, null)
        } returns listOf(
            SpecimenWithSpecimenImagesAndInferenceResults(
                specimen = Specimen(id = "AAY", remoteId = null, shouldProcessFurther = false),
                specimenImagesAndInferenceResults = images.map { SpecimenImageAndInferenceResult(it, null) },
            )
        )
        val metadataUploadStatusRepository = mockk<MetadataUploadStatusRepository>()
        every {
            metadataUploadStatusRepository.observeMetadataUploadStatusesForSession(sessionId)
        } returns flowOf(steps)
        schedulerWorkManager = mockk(relaxed = true)
        return SessionUploadScheduler(
            sessionRepository = sessionRepository,
            specimenRepository = specimenRepository,
            metadataUploadStatusRepository = metadataUploadStatusRepository,
            workManagerRepository = schedulerWorkManager,
        )
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
