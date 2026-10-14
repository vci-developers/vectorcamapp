package com.vci.vectorcamapp.core.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.room.dao.InferenceResultDao
import com.vci.vectorcamapp.core.data.room.dao.SpecimenDao
import com.vci.vectorcamapp.core.data.room.dao.SpecimenImageDao
import com.vci.vectorcamapp.core.data.room.entities.InferenceResultEntity
import com.vci.vectorcamapp.core.data.room.entities.SpecimenEntity
import com.vci.vectorcamapp.core.data.room.entities.SpecimenImageEntity
import com.vci.vectorcamapp.core.data.room.entities.relations.SpecimenImageAndInferenceResultRelation
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class SpecimenRepositoryImplementationTest {

    private val sessionId = UUID.fromString("00000000-0000-0000-0000-000000000061")
    private val unitId = UUID.fromString("00000000-0000-0000-0000-000000000062")
    private val imageUri = mockk<Uri>(relaxed = true)

    @Test
    fun inference_insertAndUpdateCoverSuccessAndFailures() = runTest {
        val dao = mockk<InferenceResultDao>()
        val repository = InferenceResultRepositoryImplementation(dao)
        val result = inference()
        coEvery { dao.insertInferenceResult(any()) } returns Unit andThenThrows SQLiteConstraintException("pk") andThenThrows
            IllegalStateException("db")
        coEvery { dao.updateInferenceResult(any()) } returnsMany listOf(1, 0) andThenThrows
            SQLiteConstraintException("pk") andThenThrows IllegalStateException("db")

        assertThat(repository.insertInferenceResult(result, "img")).isEqualTo(Result.Success(Unit))
        assertThat(repository.insertInferenceResult(result, "img"))
            .isEqualTo(Result.Error(RoomDbError.CONSTRAINT_VIOLATION))
        assertThat(repository.insertInferenceResult(result, "img"))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))

        assertThat(repository.updateInferenceResult(result, "img")).isEqualTo(Result.Success(Unit))
        assertThat(repository.updateInferenceResult(result, "img"))
            .isEqualTo(Result.Error(RoomDbError.NO_ROWS_AFFECTED))
        assertThat(repository.updateInferenceResult(result, "img"))
            .isEqualTo(Result.Error(RoomDbError.CONSTRAINT_VIOLATION))
        assertThat(repository.updateInferenceResult(result, "img"))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
    }

    @Test
    fun specimenImage_insertUpdateCounts() = runTest {
        val dao = mockk<SpecimenImageDao>()
        val repository = SpecimenImageRepositoryImplementation(dao)
        val image = specimenImage()
        coEvery { dao.insertSpecimenImage(any()) } returns Unit andThenThrows SQLiteConstraintException("pk") andThenThrows
            IllegalStateException("db")
        coEvery { dao.updateSpecimenImage(any()) } returnsMany listOf(1, 0) andThenThrows
            SQLiteConstraintException("pk") andThenThrows IllegalStateException("db")
        coEvery { dao.getTotalCountForSession(sessionId) } returns 4
        every { dao.observeUploadedMetadataCountForSession(sessionId) } returns flowOf(1)
        every { dao.observeUploadedImageCountForSession(sessionId) } returns flowOf(2)
        every { dao.observeFailedImageCountForSession(sessionId) } returns flowOf(3)

        assertThat(repository.insertSpecimenImage(image, "spec", sessionId)).isEqualTo(Result.Success(Unit))
        assertThat(repository.insertSpecimenImage(image, "spec", sessionId))
            .isEqualTo(Result.Error(RoomDbError.CONSTRAINT_VIOLATION))
        assertThat(repository.insertSpecimenImage(image, "spec", sessionId))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
        assertThat(repository.updateSpecimenImage(image, "spec", sessionId)).isEqualTo(Result.Success(Unit))
        assertThat(repository.updateSpecimenImage(image, "spec", sessionId))
            .isEqualTo(Result.Error(RoomDbError.NO_ROWS_AFFECTED))
        assertThat(repository.updateSpecimenImage(image, "spec", sessionId))
            .isEqualTo(Result.Error(RoomDbError.CONSTRAINT_VIOLATION))
        assertThat(repository.updateSpecimenImage(image, "spec", sessionId))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
        assertThat(repository.getTotalCountForSession(sessionId)).isEqualTo(4)
        repository.observeUploadedMetadataCountForSession(sessionId).test {
            assertThat(awaitItem()).isEqualTo(1)
            awaitComplete()
        }
        repository.observeUploadedImageCountForSession(sessionId).test {
            assertThat(awaitItem()).isEqualTo(2)
            awaitComplete()
        }
        repository.observeFailedImageCountForSession(sessionId).test {
            assertThat(awaitItem()).isEqualTo(3)
            awaitComplete()
        }
    }

    @Test
    fun specimen_insertUpdateAndLookups() = runTest {
        val dao = mockk<SpecimenDao>()
        val repository = SpecimenRepositoryImplementation(dao)
        val specimen = Specimen("spec", remoteId = 4, shouldProcessFurther = true)
        coEvery { dao.insertSpecimen(any()) } returns Unit andThenThrows SQLiteConstraintException("pk") andThenThrows
            IllegalStateException("db")
        coEvery { dao.updateSpecimen(any()) } returnsMany listOf(1, 0) andThenThrows
            SQLiteConstraintException("pk") andThenThrows IllegalStateException("db")
        coEvery { dao.getSpecimenByIdAndSessionId("spec", sessionId) } returns SpecimenEntity(
            id = "spec",
            sessionId = sessionId,
            sessionUnitId = unitId,
            remoteId = 4,
            shouldProcessFurther = true,
        )
        coEvery { dao.getSessionUnitIdForSpecimen("spec", sessionId) } returns unitId
        coEvery { dao.countSelectedSpecimensForFurtherProcessingBetweenSessionCollectionDates(1L, 2L) } returns 6

        assertThat(repository.insertSpecimen(specimen, sessionId, unitId)).isEqualTo(Result.Success(Unit))
        assertThat(repository.insertSpecimen(specimen, sessionId, unitId))
            .isEqualTo(Result.Error(RoomDbError.CONSTRAINT_VIOLATION))
        assertThat(repository.insertSpecimen(specimen, sessionId, unitId))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
        assertThat(repository.updateSpecimen(specimen, sessionId, unitId)).isEqualTo(Result.Success(Unit))
        assertThat(repository.updateSpecimen(specimen, sessionId, unitId))
            .isEqualTo(Result.Error(RoomDbError.NO_ROWS_AFFECTED))
        assertThat(repository.updateSpecimen(specimen, sessionId, unitId))
            .isEqualTo(Result.Error(RoomDbError.CONSTRAINT_VIOLATION))
        assertThat(repository.updateSpecimen(specimen, sessionId, unitId))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
        assertThat(repository.getSpecimenByIdAndSessionId("spec", sessionId)).isEqualTo(specimen)
        assertThat(repository.getSessionUnitIdForSpecimen("spec", sessionId)).isEqualTo(unitId)
        assertThat(
            repository.countSelectedForFurtherProcessingBetweenSessionCollectionDates(1L, 2L)
        ).isEqualTo(6)
    }

    @Test
    fun specimen_loadsImagesWithAndWithoutInference() = runTest {
        val dao = mockk<SpecimenDao>()
        val repository = SpecimenRepositoryImplementation(dao)
        val specimenEntity = SpecimenEntity(id = "spec", sessionId = sessionId, shouldProcessFurther = false)
        val withInference = SpecimenImageAndInferenceResultRelation(
            specimenImageEntity = imageEntity(),
            inferenceResultEntity = InferenceResultEntity(specimenImageId = "img", bboxClassId = 2),
        )
        val withoutInference = SpecimenImageAndInferenceResultRelation(
            specimenImageEntity = imageEntity(localId = "img-2"),
            inferenceResultEntity = null,
        )
        coEvery { dao.getSpecimensBySessionScope(sessionId, unitId) } returns listOf(specimenEntity)
        coEvery { dao.getSpecimenImagesAndInferenceResultsBySpecimen("spec", sessionId) } returns listOf(
            withInference,
            withoutInference,
        )

        val loaded = repository.getSpecimenImagesAndInferenceResultsBySessionScope(sessionId, unitId)

        assertThat(loaded).hasSize(1)
        assertThat(loaded.single().specimen.id).isEqualTo("spec")
        assertThat(loaded.single().specimenImagesAndInferenceResults).hasSize(2)
        assertThat(loaded.single().specimenImagesAndInferenceResults[0].inferenceResult?.bboxClassId).isEqualTo(2)
        assertThat(loaded.single().specimenImagesAndInferenceResults[1].inferenceResult).isNull()
    }

    @Test
    fun specimen_observeEmitsEmptyAndPopulatedGraphs() = runTest {
        val dao = mockk<SpecimenDao>()
        val repository = SpecimenRepositoryImplementation(dao)
        val specimens = kotlinx.coroutines.flow.MutableStateFlow<List<SpecimenEntity>>(emptyList())
        every { dao.observeSpecimensBySessionScope(sessionId, null) } returns specimens
        every { dao.observeSpecimenImagesAndInferenceResultsBySpecimen("spec", sessionId) } returns flowOf(
            listOf(
                SpecimenImageAndInferenceResultRelation(imageEntity(), null)
            )
        )

        repository.observeSpecimenImagesAndInferenceResultsBySessionScope(sessionId, null).test {
            assertThat(awaitItem()).isEmpty()
            specimens.value = listOf(SpecimenEntity(id = "spec", sessionId = sessionId))
            val populated = awaitItem()
            assertThat(populated.single().specimenImagesAndInferenceResults).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun inference() = InferenceResult(
        bboxTopLeftX = 0f,
        bboxTopLeftY = 0f,
        bboxWidth = 1f,
        bboxHeight = 1f,
        bboxConfidence = 0.9f,
        bboxClassId = 1,
        speciesLogits = listOf(0.1f),
        sexLogits = null,
        abdomenStatusLogits = null,
        bboxDetectionDuration = 5L,
        speciesInferenceDuration = null,
        sexInferenceDuration = null,
        abdomenStatusInferenceDuration = null,
    )

    private fun specimenImage() = SpecimenImage(
        localId = "img",
        remoteId = null,
        species = "Anopheles",
        sex = null,
        abdomenStatus = null,
        imageUri = imageUri,
        metadataUploadStatus = UploadStatus.NOT_STARTED,
        imageUploadStatus = UploadStatus.NOT_STARTED,
        capturedAt = 10L,
        submittedAt = null,
        imageMetadata = null,
    )

    private fun imageEntity(localId: String = "img") = SpecimenImageEntity(
        localId = localId,
        specimenId = "spec",
        sessionId = sessionId,
        species = "Anopheles",
        imageUri = imageUri,
        capturedAt = 10L,
    )
}
