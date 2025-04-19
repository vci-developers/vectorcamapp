package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.Specimen
import java.util.UUID

interface SpecimenRepository {
    suspend fun upsertSpecimen(specimen: Specimen, sessionId: UUID): Boolean
    suspend fun deleteSpecimen(specimen: Specimen, sessionId: UUID): Boolean
}
