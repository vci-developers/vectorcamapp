package com.vci.vectorcamapp.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.room.dao.CollectorDao
import com.vci.vectorcamapp.core.data.room.dao.LocationTypeDao
import com.vci.vectorcamapp.core.data.room.dao.ProgramDao
import com.vci.vectorcamapp.core.data.room.dao.SiteDao
import com.vci.vectorcamapp.core.data.room.dao.SurveillanceFormDao
import com.vci.vectorcamapp.core.data.room.entities.CollectorEntity
import com.vci.vectorcamapp.core.data.room.entities.LocationTypeEntity
import com.vci.vectorcamapp.core.data.room.entities.ProgramEntity
import com.vci.vectorcamapp.core.data.room.entities.SiteEntity
import com.vci.vectorcamapp.core.data.room.entities.SurveillanceFormEntity
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.LocationType
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
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

class CatalogRepositoryImplementationTest {

    @Test
    fun program_upsertGetAndObserve() = runTest {
        val dao = mockk<ProgramDao>()
        val repository = ProgramRepositoryImplementation(dao)
        val program = Program(1, "Name", "UG", "v1")
        coEvery { dao.upsertProgram(any()) } returns Unit
        coEvery { dao.getProgramById(1) } returns ProgramEntity(1, "Name", "UG", "v1")
        every { dao.observeAllPrograms() } returns flowOf(listOf(ProgramEntity(1, "Name", "UG", "v1")))

        assertThat(repository.upsertProgram(program)).isEqualTo(Result.Success(Unit))
        assertThat(repository.getProgramById(1)).isEqualTo(program)
        repository.observeAllPrograms().test {
            assertThat(awaitItem()).containsExactly(program)
            awaitComplete()
        }
    }

    @Test
    fun program_upsertFailureAndMissingRow() = runTest {
        val dao = mockk<ProgramDao>()
        val repository = ProgramRepositoryImplementation(dao)
        coEvery { dao.upsertProgram(any()) } throws IllegalStateException("db")
        coEvery { dao.getProgramById(4) } returns null

        assertThat(repository.upsertProgram(Program(4, "N", "UG", null)))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
        assertThat(repository.getProgramById(4)).isNull()
    }

    @Test
    fun site_upsertObserveAndDeactivate() = runTest {
        val dao = mockk<SiteDao>()
        val repository = SiteRepositoryImplementation(dao)
        val site = sampleSite()
        coEvery { dao.upsertSite(any()) } returns Unit
        coEvery { dao.setAllSitesInactiveForProgram(any()) } returns Unit
        every { dao.observeAllSitesByProgramId(9) } returns flowOf(listOf(sampleSiteEntity()))

        assertThat(repository.upsertSite(site, programId = 9, locationTypeId = 2, parentId = 3))
            .isEqualTo(Result.Success(Unit))
        repository.setAllSitesInactiveForProgram(9)
        repository.observeAllSitesByProgramId(9).test {
            assertThat(awaitItem()).containsExactly(site)
            awaitComplete()
        }
        coVerify { dao.setAllSitesInactiveForProgram(9) }
    }

    @Test
    fun site_upsertFailure() = runTest {
        val dao = mockk<SiteDao>()
        val repository = SiteRepositoryImplementation(dao)
        coEvery { dao.upsertSite(any()) } throws IllegalStateException("db")

        assertThat(repository.upsertSite(sampleSite(), 1, null, null))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
    }

    @Test
    fun collector_upsertDeleteAndObserve() = runTest {
        val dao = mockk<CollectorDao>()
        val repository = CollectorRepositoryImplementation(dao)
        val id = UUID.fromString("00000000-0000-0000-0000-000000000021")
        val collector = Collector(id, "Ada", "VCO", 10L)
        coEvery { dao.upsertCollector(any()) } returns 1L
        coEvery { dao.deleteCollector(any()) } returnsMany listOf(1, 0)
        every { dao.observeAllCollectors() } returns flowOf(
            listOf(CollectorEntity(id, "Ada", "VCO", 10L))
        )

        assertThat(repository.upsertCollector(collector)).isEqualTo(Result.Success(Unit))
        assertThat(repository.deleteCollector(collector)).isTrue()
        assertThat(repository.deleteCollector(collector)).isFalse()
        repository.observeAllCollectors().test {
            assertThat(awaitItem()).containsExactly(collector)
            awaitComplete()
        }
    }

    @Test
    fun collector_upsertFailure() = runTest {
        val dao = mockk<CollectorDao>()
        val repository = CollectorRepositoryImplementation(dao)
        coEvery { dao.upsertCollector(any()) } throws IllegalStateException("db")

        assertThat(repository.upsertCollector(Collector(UUID.randomUUID(), "A", "VCO", 0L)))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
    }

    @Test
    fun locationType_upsertAndObserve() = runTest {
        val dao = mockk<LocationTypeDao>()
        val repository = LocationTypeRepositoryImplementation(dao)
        val locationType = LocationType(3, "Village", 2)
        coEvery { dao.upsertLocationType(any()) } returns 1L
        every { dao.observeAllLocationTypesByProgramId(7) } returns flowOf(
            listOf(LocationTypeEntity(3, 7, "Village", 2))
        )

        assertThat(repository.upsertLocationType(locationType, 7)).isEqualTo(Result.Success(Unit))
        repository.observeAllLocationTypesByProgramId(7).test {
            assertThat(awaitItem()).containsExactly(locationType)
            awaitComplete()
        }
    }

    @Test
    fun locationType_upsertFailure() = runTest {
        val dao = mockk<LocationTypeDao>()
        val repository = LocationTypeRepositoryImplementation(dao)
        coEvery { dao.upsertLocationType(any()) } throws IllegalStateException("db")

        assertThat(repository.upsertLocationType(LocationType(1, "District", 1), 7))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
    }

    @Test
    fun surveillanceForm_upsertGetAndObserve() = runTest {
        val dao = mockk<SurveillanceFormDao>()
        val repository = SurveillanceFormRepositoryImplementation(dao)
        val sessionId = UUID.fromString("00000000-0000-0000-0000-000000000031")
        val form = sampleSurveillanceForm()
        coEvery { dao.upsertSurveillanceForm(any()) } returns 1L
        coEvery { dao.getSurveillanceFormBySessionId(sessionId) } returns sampleSurveillanceEntity(sessionId)
        every { dao.observeSurveillanceFormBySessionId(sessionId) } returns flowOf(sampleSurveillanceEntity(sessionId))

        assertThat(repository.upsertSurveillanceForm(form, sessionId)).isEqualTo(Result.Success(Unit))
        assertThat(repository.getSurveillanceFormBySessionId(sessionId)).isEqualTo(form)
        repository.observeSurveillanceFormBySessionId(sessionId).test {
            assertThat(awaitItem()).isEqualTo(form)
            awaitComplete()
        }
    }

    @Test
    fun surveillanceForm_missingAndFailedUpsert() = runTest {
        val dao = mockk<SurveillanceFormDao>()
        val repository = SurveillanceFormRepositoryImplementation(dao)
        val sessionId = UUID.randomUUID()
        coEvery { dao.upsertSurveillanceForm(any()) } throws IllegalStateException("db")
        coEvery { dao.getSurveillanceFormBySessionId(sessionId) } returns null
        every { dao.observeSurveillanceFormBySessionId(sessionId) } returns flowOf(null)

        assertThat(repository.upsertSurveillanceForm(sampleSurveillanceForm(), sessionId))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
        assertThat(repository.getSurveillanceFormBySessionId(sessionId)).isNull()
        repository.observeSurveillanceFormBySessionId(sessionId).test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    private fun sampleSite() = Site(
        id = 5,
        district = "D",
        subCounty = "S",
        parish = "P",
        villageName = "V",
        houseNumber = "1",
        healthCenter = "HC",
        isActive = true,
        name = "Site",
        locationHierarchy = mapOf("district" to "D"),
    )

    private fun sampleSiteEntity() = SiteEntity(
        id = 5,
        programId = 9,
        district = "D",
        subCounty = "S",
        parish = "P",
        villageName = "V",
        houseNumber = "1",
        healthCenter = "HC",
        isActive = true,
        locationTypeId = 2,
        parentId = 3,
        name = "Site",
        locationHierarchy = mapOf("district" to "D"),
    )

    private fun sampleSurveillanceForm() = SurveillanceForm(
        numPeopleSleptInHouse = 3,
        wasIrsConducted = true,
        monthsSinceIrs = 2,
        numLlinsAvailable = 1,
        llinType = "LLIN",
        llinBrand = "Brand",
        numPeopleSleptUnderLlin = 2,
        numChildrenUnder5 = null,
        hasPregnantWoman = null,
        submittedAt = 9L,
    )

    private fun sampleSurveillanceEntity(sessionId: UUID) = SurveillanceFormEntity(
        sessionId = sessionId,
        numPeopleSleptInHouse = 3,
        wasIrsConducted = true,
        monthsSinceIrs = 2,
        numLlinsAvailable = 1,
        llinType = "LLIN",
        llinBrand = "Brand",
        numPeopleSleptUnderLlin = 2,
        numChildrenUnder5 = null,
        hasPregnantWoman = null,
        submittedAt = 9L,
    )
}
