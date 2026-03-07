package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.SurveillanceFormDao
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.repository.SurveillanceFormRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class SurveillanceFormRepositoryImplementation @Inject constructor(
    private val surveillanceFormDao: SurveillanceFormDao
) : SurveillanceFormRepository {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun upsertSurveillanceForm(
        surveillanceForm: SurveillanceForm, sessionId: UUID
    ): Result<Unit, RoomDbError> {
        return try {
            surveillanceFormDao.upsertSurveillanceForm(surveillanceForm.toEntity(sessionId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override suspend fun getSurveillanceFormBySessionId(sessionId: UUID): SurveillanceForm? {
        return surveillanceFormDao.getSurveillanceFormBySessionId(sessionId)?.toDomain()
    }

    override fun observeSurveillanceFormBySessionId(sessionId: UUID): Flow<SurveillanceForm?> {
        return surveillanceFormDao.observeSurveillanceFormBySessionId(sessionId).map { it?.toDomain() }
    }
}
