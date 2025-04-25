package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenAndBoundingBox
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SpecimenRepository {
    suspend fun upsertSpecimen(specimen: Specimen, sessionId: UUID): Boolean
    suspend fun deleteSpecimen(specimen: Specimen, sessionId: UUID): Boolean
    fun observeSpecimenAndBoundingBox(specimenId: String): Flow<SpecimenAndBoundingBox?>
}
