package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.DataRecoverySubmissionDao
import com.vci.vectorcamapp.core.domain.model.DataRecoverySubmission
import com.vci.vectorcamapp.core.domain.repository.DataRecoverySubmissionRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import java.util.UUID
import javax.inject.Inject

class DataRecoverySubmissionRepositoryImplementation @Inject constructor(
    private val dataRecoverySubmissionDao: DataRecoverySubmissionDao
) : DataRecoverySubmissionRepository {

    override suspend fun recordDataRecoverySubmission(
        dataRecoverySubmission: DataRecoverySubmission,
        sessionId: UUID,
    ): Result<Unit, RoomDbError> {
        return try {
            dataRecoverySubmissionDao.upsertDataRecoverySubmission(
                dataRecoverySubmission.toEntity(sessionId)
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override suspend fun getSubmittedIdempotencyKeysForSession(sessionId: UUID): Set<String> {
        return dataRecoverySubmissionDao.getIdempotencyKeysForSession(sessionId).toSet()
    }
}
