package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.DataRecoverySubmissionEntity
import java.util.UUID

@Dao
interface DataRecoverySubmissionDao {

    @Upsert
    suspend fun upsertDataRecoverySubmission(dataRecoverySubmission: DataRecoverySubmissionEntity)

    @Query("SELECT idempotencyKey FROM data_recovery_submission WHERE sessionId = :sessionId")
    suspend fun getIdempotencyKeysForSession(sessionId: UUID): List<String>
}
