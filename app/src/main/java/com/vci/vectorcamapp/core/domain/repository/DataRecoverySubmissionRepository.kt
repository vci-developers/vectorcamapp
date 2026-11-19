package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.DataRecoverySubmission
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import java.util.UUID

interface DataRecoverySubmissionRepository {
    suspend fun recordDataRecoverySubmission(
        dataRecoverySubmission: DataRecoverySubmission,
        sessionId: UUID,
    ): Result<Unit, RoomDbError>
    suspend fun getSubmittedIdempotencyKeysForSession(sessionId: UUID): Set<String>
}
