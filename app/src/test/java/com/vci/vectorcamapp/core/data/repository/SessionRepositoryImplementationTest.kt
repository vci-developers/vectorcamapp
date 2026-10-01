package com.vci.vectorcamapp.core.data.repository

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.room.dao.SessionDao
import com.vci.vectorcamapp.core.data.room.entities.SessionEntity
import com.vci.vectorcamapp.core.data.room.entities.SiteEntity
import com.vci.vectorcamapp.core.data.room.entities.SurveillanceFormEntity
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionAndSiteRelation
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionAndSurveillanceFormRelation
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class SessionRepositoryImplementationTest {

    private val sessionId = UUID.fromString("00000000-0000-0000-0000-000000000051")
    private val dao = mockk<SessionDao>()
    private val repository = SessionRepositoryImplementation(dao)

    @Test
    fun upsertGetDeleteAndComplete() = runTest {
        coEvery { dao.upsertSession(any()) } returns 1L
        coEvery { dao.getSessionById(sessionId) } returns sessionEntity()
        coEvery { dao.deleteSession(any()) } returnsMany listOf(1, 0)
        coEvery { dao.markSessionAsComplete(sessionId, any()) } returns 1
        coEvery { dao.getImageUrisBySessionId(sessionId) } returns listOf(mockk<Uri>())

        assertThat(repository.upsertSession(session(), 5)).isEqualTo(Result.Success(Unit))
        assertThat(repository.getSessionById(sessionId)).isEqualTo(session())
        assertThat(repository.deleteSession(session(), 5)).isTrue()
        assertThat(repository.deleteSession(session(), 5)).isFalse()
        assertThat(repository.markSessionAsComplete(sessionId)).isTrue()
        assertThat(repository.getImageUrisBySessionId(sessionId)).hasSize(1)
        coVerify { dao.markSessionAsComplete(sessionId, any()) }
    }

    @Test
    fun upsertFailureAndMissingSession() = runTest {
        coEvery { dao.upsertSession(any()) } throws IllegalStateException("db")
        coEvery { dao.getSessionById(sessionId) } returns null
        coEvery { dao.markSessionAsComplete(sessionId, any()) } returns 0
        coEvery { dao.getSessionAndSurveillanceForm(sessionId) } returns null
        coEvery { dao.getSessionAndSiteById(sessionId) } returns null

        assertThat(repository.upsertSession(session(), 5)).isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
        assertThat(repository.getSessionById(sessionId)).isNull()
        assertThat(repository.markSessionAsComplete(sessionId)).isFalse()
        assertThat(repository.getSessionAndSurveillanceFormById(sessionId)).isNull()
        assertThat(repository.getSessionAndSiteById(sessionId)).isNull()
    }

    @Test
    fun surveillanceRelation_mapsPresentAndMissingForm() = runTest {
        coEvery { dao.getSessionAndSurveillanceForm(sessionId) } returns SessionAndSurveillanceFormRelation(
            sessionEntity = sessionEntity(),
            surveillanceFormEntity = SurveillanceFormEntity(sessionId = sessionId, numPeopleSleptInHouse = 2),
        )

        val withForm = repository.getSessionAndSurveillanceFormById(sessionId)
        assertThat(withForm?.session).isEqualTo(session())
        assertThat(withForm?.surveillanceForm).isEqualTo(
            SurveillanceForm(
                numPeopleSleptInHouse = 2,
                wasIrsConducted = false,
                monthsSinceIrs = null,
                numLlinsAvailable = 0,
                llinType = null,
                llinBrand = null,
                numPeopleSleptUnderLlin = null,
                numChildrenUnder5 = null,
                hasPregnantWoman = null,
                submittedAt = null,
            )
        )

        coEvery { dao.getSessionAndSurveillanceForm(sessionId) } returns SessionAndSurveillanceFormRelation(
            sessionEntity = sessionEntity(),
            surveillanceFormEntity = null,
        )
        assertThat(repository.getSessionAndSurveillanceFormById(sessionId)?.surveillanceForm).isNull()
    }

    @Test
    fun siteRelations_mapCompleteIncompleteAndSnapshot() = runTest {
        val relation = SessionAndSiteRelation(sessionEntity(), siteEntity())
        coEvery { dao.getSessionAndSiteById(sessionId) } returns relation
        coEvery { dao.getIncompleteSessionsAndSites() } returns listOf(relation)
        every { dao.observeCompleteSessionsAndSites() } returns flowOf(listOf(relation))
        every { dao.observeIncompleteSessionsAndSites() } returns flowOf(emptyList())

        val loaded = repository.getSessionAndSiteById(sessionId)
        assertThat(loaded?.session).isEqualTo(session())
        assertThat(loaded?.site).isEqualTo(site())
        assertThat(repository.getIncompleteSessionsAndSites().single().site.name).isEqualTo("Site")
        repository.observeCompleteSessionsAndSites().test {
            assertThat(awaitItem()).hasSize(1)
            awaitComplete()
        }
        repository.observeIncompleteSessionsAndSites().test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
    }

    private fun session() = Session(
        localId = sessionId,
        remoteId = null,
        hardwareId = "HW",
        collectorTitle = "VCO",
        collectorName = "Ada",
        collectorLastTrainedOn = 1L,
        collectionDate = 2L,
        collectionMethod = "Net",
        specimenCondition = "Good",
        createdAt = 3L,
        completedAt = null,
        submittedAt = null,
        notes = "",
        latitude = 0.1f,
        longitude = 0.2f,
        type = SessionType.SURVEILLANCE,
    )

    private fun sessionEntity() = SessionEntity(
        localId = sessionId,
        siteId = 5,
        hardwareId = "HW",
        collectorTitle = "VCO",
        collectorName = "Ada",
        collectorLastTrainedOn = 1L,
        collectionDate = 2L,
        collectionMethod = "Net",
        specimenCondition = "Good",
        createdAt = 3L,
        latitude = 0.1f,
        longitude = 0.2f,
        type = SessionType.SURVEILLANCE,
    )

    private fun site() = Site(5, "D", null, null, "V", "1", null, true, "Site", null)

    private fun siteEntity() = SiteEntity(
        id = 5,
        district = "D",
        villageName = "V",
        houseNumber = "1",
        isActive = true,
        name = "Site",
    )
}
