package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.SpecimenDao
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenAndBoundingBox
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class SpecimenRepositoryImplementation @Inject constructor(
    private val specimenDao: SpecimenDao
) : SpecimenRepository {
    override suspend fun upsertSpecimen(specimen: Specimen, sessionId: UUID): Boolean {
        return specimenDao.upsertSpecimen(specimen.toEntity(sessionId)) != -1L
    }

    override suspend fun deleteSpecimen(specimen: Specimen, sessionId: UUID): Boolean {
        return specimenDao.deleteSpecimen(specimen.toEntity(sessionId)) > 0
    }

    override fun observeSpecimenAndBoundingBox(specimenId: String): Flow<SpecimenAndBoundingBox?> {
        return specimenDao.observeSpecimenAndBoundingBox(specimenId).map { specimenAndBoundingBoxRelation ->
            specimenAndBoundingBoxRelation?.let {
                SpecimenAndBoundingBox(
                    specimen = it.specimenEntity.toDomain(),
                    boundingBox = it.boundingBoxEntity.toDomain()
                )
            }
        }
    }
}
